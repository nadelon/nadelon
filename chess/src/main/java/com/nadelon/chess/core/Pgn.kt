package com.nadelon.chess.core

/** One half-move in a parsed game, with positions before and after it. */
class Ply(
    val ply: Int,            // 0-based half-move index
    val sideToMove: Int,     // Board.WHITE / Board.BLACK (who made this move)
    val san: String,
    val move: Move,
    val fenBefore: String,
    val fenAfter: String
)

/** A fully parsed game: PGN tags plus the replayed move list. */
class ParsedGame(
    val tags: Map<String, String>,
    val plies: List<Ply>,
    val result: String
) {
    val white: String get() = tags["White"] ?: "?"
    val black: String get() = tags["Black"] ?: "?"
    val whiteElo: Int? get() = tags["WhiteElo"]?.toIntOrNull()
    val blackElo: Int? get() = tags["BlackElo"]?.toIntOrNull()
    val eco: String? get() = tags["ECO"]
    val openingTag: String? get() = tags["Opening"] ?: tags["ECOUrl"]
    val date: String get() = tags["Date"] ?: tags["UTCDate"] ?: ""
    val timeControl: String get() = tags["TimeControl"] ?: ""
}

object Pgn {

    private val TAG_RE = Regex("""\[(\w+)\s+"([^"]*)"\]""")

    /** Split a multi-game PGN string into individual game blocks. */
    fun splitGames(pgn: String): List<String> {
        val games = ArrayList<String>()
        val lines = pgn.replace("\r\n", "\n").split("\n")
        val current = StringBuilder()
        var sawMovesAfterTags = false
        var inTags = false
        for (line in lines) {
            val isTag = line.startsWith("[")
            if (isTag && sawMovesAfterTags) {
                // start of a new game
                games.add(current.toString())
                current.clear()
                sawMovesAfterTags = false
            }
            if (!isTag && line.isNotBlank()) sawMovesAfterTags = true
            current.append(line).append('\n')
            inTags = isTag
        }
        if (current.isNotBlank()) games.add(current.toString())
        return games.filter { it.contains("[") }
    }

    fun parse(pgnGame: String): ParsedGame? {
        val tags = LinkedHashMap<String, String>()
        for (m in TAG_RE.findAll(pgnGame)) {
            tags[m.groupValues[1]] = m.groupValues[2]
        }
        // movetext = everything not in a tag line
        val moveText = pgnGame.lines()
            .filterNot { it.trimStart().startsWith("[") }
            .joinToString(" ")
        val tokens = tokenize(moveText)
        val board = Board.startingPosition()
        val plies = ArrayList<Ply>()
        var result = tags["Result"] ?: "*"
        var idx = 0
        for (tok in tokens) {
            if (tok == "1-0" || tok == "0-1" || tok == "1/2-1/2" || tok == "*") {
                result = tok
                continue
            }
            val fenBefore = board.toFen()
            val side = board.turn
            val mv = board.moveFromSan(tok) ?: break // malformed game; stop replay
            val san = board.san(mv, board.legalMoves())
            board.make(mv)
            plies.add(Ply(idx, side, san, mv, fenBefore, board.toFen()))
            idx++
        }
        if (plies.isEmpty() && tags.isEmpty()) return null
        return ParsedGame(tags, plies, result)
    }

    /** Strip move numbers, comments, NAGs and variations, leaving SAN/result tokens. */
    private fun tokenize(text: String): List<String> {
        var s = text
        // remove comments { ... }
        s = s.replace(Regex("""\{[^}]*\}"""), " ")
        // remove ; line comments
        s = s.replace(Regex(""";[^\n]*"""), " ")
        // remove recursive variations ( ... ) — do a few passes for nesting
        repeat(6) { s = s.replace(Regex("""\([^()]*\)"""), " ") }
        // remove NAGs like $1
        s = s.replace(Regex("""\$\d+"""), " ")
        val out = ArrayList<String>()
        for (raw in s.split(Regex("\\s+"))) {
            var t = raw.trim()
            if (t.isEmpty()) continue
            // strip move numbers: "12." "12..." possibly glued to a move "12.e4"
            t = t.replace(Regex("""^\d+\.+"""), "")
            if (t.isEmpty()) continue
            if (t == "1-0" || t == "0-1" || t == "1/2-1/2" || t == "*") { out.add(t); continue }
            // ignore stray dots
            if (t.all { it == '.' }) continue
            out.add(t)
        }
        return out
    }
}
