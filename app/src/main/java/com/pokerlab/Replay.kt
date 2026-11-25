package com.pokerlab

import kotlin.math.max

class RpPlayer(var st: Double, var bet: Double, var f: Boolean, var c: List<String>?) {
    fun copy() = RpPlayer(st, bet, f, c)
}

class RpStep(
    val t: String, val a: String?, val street: String, val bd: List<String>,
    val pot: Double, val P: Map<String, RpPlayer>
)

class RpData(val seats: List<Pair<Int, String>>, val bt: Int, val steps: List<RpStep>)

private val RSEAT = Regex("""^Seat (\d+): (\S+) \(([\d.,]+) in chips\)""")
private val RSTREET = Regex("""^\*\*\* (FLOP|TURN|RIVER) \*\*\*.*?\[([^\]]*)\]\s*$""")
private val RPOST2 = Regex("""^(\S+): posts (?:the )?(ante|small blind|big blind) ([\d.,]+)""")
private val RSHOW2 = Regex("""^(\S+): shows \[(\w\w) (\w\w)\]""")
private val RCOLL = Regex("""^(\S+) collected ([\d.,]+) from""")
private val RBTN = Regex("""Seat #(\d+) is the button""")
private val WS2 = Regex("\\s+")

fun buildReplay(h: Hand): RpData {
    val L = h.raw.split("\n")
    val bt = RBTN.find(L.getOrElse(1) { "" })?.groupValues?.get(1)?.toInt() ?: 0
    val seats = ArrayList<Pair<Int, String>>()
    val P = LinkedHashMap<String, RpPlayer>()
    val steps = ArrayList<RpStep>()
    val bd = ArrayList<String>()
    var street = "Preflop"
    var pot = 0.0
    var go = false
    for (l in L) {
        val m = RSEAT.find(l)
        if (m != null) {
            seats.add(Pair(m.groupValues[1].toInt(), m.groupValues[2]))
            P[m.groupValues[2]] = RpPlayer(num(m.groupValues[3]), 0.0, false, null)
        }
    }
    h.cards?.let { P["Hero"]?.c = it }
    fun u(v: Double) = f1(v / h.bb)
    fun sn(t: String, a: String? = null) {
        steps.add(RpStep(t, a, street, bd.toList(), pot + P.values.sumOf { it.bet }, P.mapValues { it.value.copy() }))
    }
    fun sw() { for (p in P.values) { pot += p.bet; p.bet = 0.0 } }
    for (l in L) {
        val xs = RSTREET.find(l)
        if (xs != null) {
            if (!go) { sn("Ciegas puestas"); go = true }
            sw()
            street = when (xs.groupValues[1]) { "FLOP" -> "Flop"; "TURN" -> "Turn"; else -> "River" }
            val cs = xs.groupValues[2].trim().split(WS2)
            if (xs.groupValues[1] == "FLOP") bd.clear()
            bd.addAll(cs)
            sn("Sale el $street: ${cs.joinToString(" ")}")
            continue
        }
        val xp = RPOST2.find(l)
        if (xp != null) {
            val a = num(xp.groupValues[3]); val o = P[xp.groupValues[1]]
            if (o != null) {
                o.st -= a
                if (xp.groupValues[2] == "ante") pot += a else o.bet += a
            }
            continue
        }
        val xsh = RSHOW2.find(l)
        if (xsh != null) {
            val o = P[xsh.groupValues[1]]
            if (o != null) {
                o.c = listOf(xsh.groupValues[2], xsh.groupValues[3])
                sn(xsh.groupValues[1] + " muestra " + xsh.groupValues[2] + " " + xsh.groupValues[3], xsh.groupValues[1])
            }
            continue
        }
        val xc = RCOLL.find(l)
        if (xc != null) {
            val o = P[xc.groupValues[1]]
            if (o != null) {
                val a = num(xc.groupValues[2]); sw(); o.st += a; pot = max(0.0, pot - a)
                sn(xc.groupValues[1] + " gana " + u(a) + " bb", xc.groupValues[1])
            }
            continue
        }
        val xa = RACT.find(l) ?: continue
        val g = xa.groupValues
        val o = P[g[1]] ?: continue
        if (!go) { sn("Ciegas puestas"); go = true }
        val p = g[1]
        if (g[8].isNotEmpty()) { val a = num(g[8]); o.st += a; o.bet -= a; continue }
        val t: String
        if (g[2] == "folds") { o.f = true; t = "se retira" }
        else if (g[2] == "checks") t = "pasa (check)"
        else if (g[3].isNotEmpty()) { val a = num(g[3]); o.st -= a; o.bet += a; t = "iguala " + u(a) + " bb" }
        else if (g[4].isNotEmpty()) { val a = num(g[4]); o.st -= a; o.bet = a; t = "apuesta " + u(a) + " bb" }
        else if (g[5].isNotEmpty()) { val to = num(g[6]); o.st -= to - o.bet; o.bet = to; t = "sube a " + u(to) + " bb" }
        else { val a = num(g[7]); o.st -= a; o.bet += a; t = "ALL-IN " + u(a) + " bb" }
        sn("$p $t", p)
    }
    if (steps.isEmpty()) sn("Mano sin acciones")
    return RpData(seats, bt, steps)
}

/** Un paso se puede enviar al solver si es una calle nueva con exactamente 2 jugadores activos y sin apuestas pendientes. */
fun rpCanSolve(s: RpStep): Boolean {
    if (s.bd.size < 3 || !s.t.startsWith("Sale el ")) return false
    val live = s.P.values.filter { !it.f }
    return live.size == 2 && live.all { it.bet < 1e-6 && it.st > 0 }
}
