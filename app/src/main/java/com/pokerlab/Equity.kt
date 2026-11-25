package com.pokerlab

import kotlin.math.max
import kotlin.math.min

fun CI(x: String): Int = "23456789TJQKA".indexOf(x[0]) * 4 + "cdhs".indexOf(x[1])

private fun straightTop(m: Int): Int {
    val r = (0..12).map { if ((m shr it) and 1 == 1) 1 else 0 }
    for (i in 12 downTo 3) if (r[i] + r[i - 1] + r[i - 2] + r[i - 3] + r[i - 4] == 5) return i
    if (r[12] + r[0] + r[1] + r[2] + r[3] == 5) return 3
    return -1
}

fun rank7(c: IntArray): Int {
    val cnt = IntArray(13)
    val suit = IntArray(4)
    for (x in c) {
        cnt[x / 4]++
        suit[x % 4]++
    }
    var ranks = 0
    for (i in 0..12) if (cnt[i] > 0) ranks = ranks or (1 shl i)
    var flushSuit = -1
    for (s in 0..3) if (suit[s] >= 5) {
        flushSuit = s
        break
    }
    if (flushSuit >= 0) {
        var fm = 0
        for (x in c) if (x % 4 == flushSuit) fm = fm or (1 shl (x / 4))
        val st = straightTop(fm)
        if (st >= 0) return (8 shl 20) or (st shl 16)
        var v = 0
        var bits = 0
        for (i in 12 downTo 0) if ((fm shr i) and 1 == 1) {
            v = (v shl 4) or i
            if (++bits == 5) break
        }
        return (5 shl 20) or v
    }
    val st = straightTop(ranks)
    if (st >= 0) return (4 shl 20) or (st shl 16)
    val fours = (0..12).filter { cnt[it] == 4 }
    val trips = (0..12).filter { cnt[it] == 3 }
    val pairs = (0..12).filter { cnt[it] == 2 }
    val singles = (0..12).filter { cnt[it] == 1 }
    if (fours.isNotEmpty()) {
        val k = fours[0]
        val kicker = (trips + pairs + singles).maxOrNull() ?: 0
        return (7 shl 20) or (k shl 16) or kicker
    }
    if (trips.size >= 2 || (trips.isNotEmpty() && pairs.isNotEmpty())) {
        val t = trips.maxOrNull()!!
        val p = (pairs + trips.filter { it != t }).maxOrNull() ?: 0
        return (6 shl 20) or (t shl 16) or p
    }
    if (trips.isNotEmpty()) {
        val t = trips[0]
        val kick = singles.sortedDescending().take(2)
        return (3 shl 20) or (t shl 16) or ((kick.getOrElse(0) { 0 } shl 8) or kick.getOrElse(1) { 0 })
    }
    if (pairs.size >= 2) {
        val ps = pairs.sortedDescending()
        val kicker = singles.maxOrNull() ?: 0
        return (2 shl 20) or (ps[0] shl 16) or (ps[1] shl 8) or kicker
    }
    if (pairs.size == 1) {
        val p = pairs[0]
        val kick = singles.sortedDescending().take(3)
        return (1 shl 20) or (p shl 16) or ((kick.getOrElse(0) { 0 } shl 8) or (kick.getOrElse(1) { 0 } shl 4) or kick.getOrElse(2) { 0 })
    }
    val kick = singles.sortedDescending().take(5)
    var v = 0
    for (k in kick) v = (v shl 4) or k
    return v
}

fun equityMulti(
    holes: List<List<String>>,
    board: List<String>,
    dead: List<String> = emptyList(),
    trials: Int = 5000
): DoubleArray {
    val n = holes.size
    val known = mutableSetOf<Int>()
    val holeCards = holes.map { h -> h.map { CI(it) }.also { known.addAll(it) } }
    val boardCards = board.map { CI(it) }.also { known.addAll(it) }
    dead.map { CI(it) }.forEach { known.add(it) }
    val remaining = (0..51).filter { it !in known }.toMutableList()
    val wins = DoubleArray(n)
    val need = 5 - boardCards.size
    if (need < 0) return wins
    val rnd = java.util.Random(42)
    repeat(trials) {
        remaining.shuffle(rnd)
        val fullBoard = boardCards + remaining.take(need)
        val scores = holeCards.map { rank7((it + fullBoard).toIntArray()) }
        val best = scores.maxOrNull()!!
        val winners = scores.withIndex().filter { it.value == best }.map { it.index }
        val share = 1.0 / winners.size
        for (w in winners) wins[w] += share
    }
    for (i in wins.indices) wins[i] /= trials
    return wins
}

fun sidePotEV(
    stacks: IntArray,
    holes: List<List<String>>,
    board: List<String>,
    pot: Int,
    trials: Int = 3000
): DoubleArray {
    val n = stacks.size
    val eq = equityMulti(holes, board, trials = trials)
    val order = stacks.withIndex().sortedBy { it.value }.map { it.index }
    val ev = DoubleArray(n)
    var prev = 0
    var remainingPot = pot.toDouble()
    for (i in order.indices) {
        val idx = order[i]
        val layer = stacks[idx] - prev
        if (layer <= 0) continue
        val contributors = order.drop(i)
        val layerSize = layer * contributors.size
        val share = layerSize.toDouble()
        for (c in contributors) {
            ev[c] += eq[c] * share
        }
        remainingPot -= share
        prev = stacks[idx]
    }
    if (remainingPot > 0) {
        for (i in 0 until n) ev[i] += eq[i] * remainingPot
    }
    return ev
}
