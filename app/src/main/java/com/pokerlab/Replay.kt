package com.pokerlab

data class ReplayStep(
    val street: String,
    val action: String,
    val actor: String,
    val amount: Int = 0,
    val pot: Int = 0,
    val board: List<String> = emptyList(),
    val stacks: Map<String, Int> = emptyMap(),
    val hole: Map<String, List<String>> = emptyMap(),
    val note: String = ""
)

fun buildReplay(hand: ParsedHand): List<ReplayStep> {
    val steps = mutableListOf<ReplayStep>()
    val stacks = hand.players.associate { it.name to it.stack }.toMutableMap()
    val holes = hand.players.associate { it.name to it.hole }.toMutableMap()
    var pot = hand.players.sumOf { it.posted }
    var board = listOf<String>()
    steps += ReplayStep("Pre", "start", "", pot = pot, board = board, stacks = stacks.toMap(), hole = holes.toMap())

    fun applyAction(street: String, a: Action) {
        when (a.type) {
            "folds" -> {
                steps += ReplayStep(street, "fold", a.player, pot = pot, board = board, stacks = stacks.toMap(), hole = holes.toMap())
            }
            "checks" -> {
                steps += ReplayStep(street, "check", a.player, pot = pot, board = board, stacks = stacks.toMap(), hole = holes.toMap())
            }
            "calls" -> {
                val amt = a.amount
                stacks[a.player] = (stacks[a.player] ?: 0) - amt
                pot += amt
                steps += ReplayStep(street, "call", a.player, amt, pot, board, stacks.toMap(), holes.toMap())
            }
            "bets", "raises" -> {
                val amt = a.amount
                stacks[a.player] = (stacks[a.player] ?: 0) - amt
                pot += amt
                steps += ReplayStep(street, a.type.removeSuffix("s"), a.player, amt, pot, board, stacks.toMap(), holes.toMap())
            }
            "all-in" -> {
                val amt = a.amount
                stacks[a.player] = 0
                pot += amt
                steps += ReplayStep(street, "all-in", a.player, amt, pot, board, stacks.toMap(), holes.toMap())
            }
            else -> {}
        }
    }

    for (a in hand.preflop) applyAction("Pre", a)
    if (hand.flop.isNotEmpty()) {
        board = hand.flop
        steps += ReplayStep("Flop", "deal", "", pot = pot, board = board, stacks = stacks.toMap(), hole = holes.toMap())
        for (a in hand.flopActions) applyAction("Flop", a)
    }
    if (hand.turn.isNotEmpty()) {
        board = hand.flop + hand.turn
        steps += ReplayStep("Turn", "deal", "", pot = pot, board = board, stacks = stacks.toMap(), hole = holes.toMap())
        for (a in hand.turnActions) applyAction("Turn", a)
    }
    if (hand.river.isNotEmpty()) {
        board = hand.flop + hand.turn + hand.river
        steps += ReplayStep("River", "deal", "", pot = pot, board = board, stacks = stacks.toMap(), hole = holes.toMap())
        for (a in hand.riverActions) applyAction("River", a)
    }
    if (hand.winners.isNotEmpty()) {
        steps += ReplayStep("Show", "showdown", hand.winners.joinToString(", "), pot = pot, board = board, stacks = stacks.toMap(), hole = holes.toMap(), note = hand.winnerNote)
    }
    return steps
}
