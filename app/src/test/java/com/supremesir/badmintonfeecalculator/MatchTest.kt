package com.supremesir.badmintonfeecalculator

import org.junit.Test

class MatchTest {

    fun generateMatches(players: List<Player>): List<Match> {
        if (players.size < 4) {
            throw IllegalArgumentException("At least 4 players are required.")
        }

        val matches = mutableListOf<Match>()
        val playerPairs = mutableSetOf<Pair<Player, Player>>()

        // Generate all unique pairs of players
        for (i in players.indices) {
            for (j in i + 1 until players.size) {
                playerPairs.add(Pair(players[i], players[j]))
            }
        }

        val allPairs = playerPairs.toMutableList()

        while (allPairs.isNotEmpty()) {
            val usedPlayers = mutableSetOf<Player>()
            var team1: Pair<Player, Player>? = null
            var team2: Pair<Player, Player>? = null

            val iterator = allPairs.iterator()
            while (iterator.hasNext()) {
                val pair = iterator.next()
                if (team1 == null && pair.first !in usedPlayers && pair.second !in usedPlayers) {
                    team1 = pair
                    usedPlayers.add(pair.first)
                    usedPlayers.add(pair.second)
                    iterator.remove()
                } else if (team2 == null && pair.first !in usedPlayers && pair.second !in usedPlayers) {
                    team2 = pair
                    usedPlayers.add(pair.first)
                    usedPlayers.add(pair.second)
                    iterator.remove()
                }

                if (team1 != null && team2 != null) break
            }

            if (team1 != null && team2 != null) {
                matches.add(Match(team1, team2))
            } else {
                break
            }
        }

        return matches
    }

    @Test
    fun testMatch() {
        val players = listOf(
            Player("1"),
            Player("2"),
            Player("3"),
            Player("4"),
            Player("5"),
            Player("6"),
            Player("7"),
            Player("8")
        )

        println("生成的比赛对阵表：")
        val matches = generateMatches(players)
        matches.forEach { match ->
            println("${match.team1.first.name} & ${match.team1.second.name} vs ${match.team2.first.name} & ${match.team2.second.name}")
        }
    }
}