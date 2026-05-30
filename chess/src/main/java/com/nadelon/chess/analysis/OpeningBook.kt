package com.nadelon.chess.analysis

import com.nadelon.chess.core.ParsedGame

/** Resolved opening identity for a game. */
class OpeningId(val eco: String, val name: String, val family: String)

/**
 * Identifies the opening. chess.com PGNs already carry [ECO] and [ECOUrl] tags,
 * so those are preferred; otherwise we fall back to a small built-in book keyed
 * on the opening moves.
 */
object OpeningBook {

    // Fallback book: SAN prefix (space separated) -> readable name.
    private val BOOK: List<Pair<String, String>> = listOf(
        "e4 c5" to "Sicilian Defense",
        "e4 e5 Nf3 Nc6 Bb5" to "Ruy Lopez",
        "e4 e5 Nf3 Nc6 Bc4" to "Italian Game",
        "e4 e5 Nf3 Nc6 d4" to "Scotch Game",
        "e4 e5 Nf3 Nf6" to "Petrov Defense",
        "e4 e5 Nf3 d6" to "Philidor Defense",
        "e4 e5 Bc4" to "Bishop's Opening",
        "e4 e5 Nc3" to "Vienna Game",
        "e4 e5 f4" to "King's Gambit",
        "e4 e5" to "Open Game",
        "e4 e6" to "French Defense",
        "e4 c6" to "Caro-Kann Defense",
        "e4 d5" to "Scandinavian Defense",
        "e4 d6" to "Pirc Defense",
        "e4 g6" to "Modern Defense",
        "e4 Nf6" to "Alekhine Defense",
        "d4 d5 c4 e6" to "Queen's Gambit Declined",
        "d4 d5 c4 c6" to "Slav Defense",
        "d4 d5 c4 dxc4" to "Queen's Gambit Accepted",
        "d4 d5 c4" to "Queen's Gambit",
        "d4 d5 Nf3" to "Queen's Pawn Game",
        "d4 Nf6 c4 e6 Nc3 Bb4" to "Nimzo-Indian Defense",
        "d4 Nf6 c4 e6" to "Indian Defense",
        "d4 Nf6 c4 g6 Nc3 d5" to "Grünfeld Defense",
        "d4 Nf6 c4 g6" to "King's Indian Defense",
        "d4 Nf6 c4 c5" to "Benoni Defense",
        "d4 Nf6 c4" to "Indian Game",
        "d4 f5" to "Dutch Defense",
        "d4 Nf6" to "Indian Defense",
        "d4 d5" to "Queen's Pawn Game",
        "c4" to "English Opening",
        "Nf3 d5 g3" to "Réti Opening",
        "Nf3" to "Réti Opening",
        "b3" to "Larsen's Opening",
        "g3" to "King's Fianchetto Opening",
        "f4" to "Bird's Opening"
    )

    fun identify(game: ParsedGame): OpeningId {
        val eco = game.eco ?: "—"
        val tagName = game.tags["Opening"]?.takeIf { it.isNotBlank() }
            ?: game.tags["ECOUrl"]?.let { slugToName(it) }
        val moves = game.plies.take(12).joinToString(" ") { it.san.trimEnd('+', '#') }
        val bookName = BOOK.firstOrNull { moves.startsWith(it.first) }?.second
        val name = (tagName ?: bookName ?: "Uncommon opening").let(::cleanName)
        return OpeningId(eco, name, familyOf(name))
    }

    private fun slugToName(url: String): String? {
        val slug = url.substringAfterLast("/openings/").substringBefore("?")
        if (slug.isBlank() || slug == url) return null
        // chess.com slugs look like "Sicilian-Defense-Najdorf-Variation...-2.Nf3-d6"
        return slug.substringBefore("...").replace("-", " ").trim()
            .split(" ").filter { it.isNotBlank() && !it.matches(Regex("\\d+\\..*")) }
            .joinToString(" ")
    }

    private fun cleanName(n: String): String =
        n.replace(Regex("\\s+"), " ").trim().take(60)

    /** The broad family used to group variations together. */
    private fun familyOf(name: String): String {
        val n = name.lowercase()
        return when {
            "sicilian" in n -> "Sicilian Defense"
            "ruy lopez" in n || "spanish" in n -> "Ruy Lopez"
            "italian" in n || "giuoco" in n -> "Italian Game"
            "french" in n -> "French Defense"
            "caro" in n -> "Caro-Kann Defense"
            "scandinavian" in n -> "Scandinavian Defense"
            "queen's gambit" in n || "queens gambit" in n -> "Queen's Gambit"
            "slav" in n -> "Slav Defense"
            "king's indian" in n || "kings indian" in n -> "King's Indian Defense"
            "nimzo" in n -> "Nimzo-Indian Defense"
            "grünfeld" in n || "grunfeld" in n -> "Grünfeld Defense"
            "english" in n -> "English Opening"
            "réti" in n || "reti" in n -> "Réti Opening"
            "london" in n -> "London System"
            "pirc" in n -> "Pirc Defense"
            "scotch" in n -> "Scotch Game"
            "vienna" in n -> "Vienna Game"
            "petrov" in n || "russian" in n -> "Petrov Defense"
            "dutch" in n -> "Dutch Defense"
            else -> name.split(",", ":").first().trim().ifBlank { name }
        }
    }
}
