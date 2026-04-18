package com.nadelon.app.data

import com.nadelon.app.model.SubtitleCue
import java.io.BufferedReader

object SubtitleParser {

    fun parse(reader: BufferedReader, filename: String?): List<SubtitleCue> {
        val text = reader.readText().replace("\uFEFF", "")
        val looksLikeVtt = text.trimStart().startsWith("WEBVTT") ||
            (filename?.endsWith(".vtt", ignoreCase = true) == true)
        return if (looksLikeVtt) parseVtt(text) else parseSrt(text)
    }

    private val TIMING_SRT = Regex(
        """(\d+):(\d{2}):(\d{2})[,.](\d{1,3})\s*-->\s*(\d+):(\d{2}):(\d{2})[,.](\d{1,3})"""
    )
    private val TIMING_VTT = Regex(
        """(?:(\d+):)?(\d{1,2}):(\d{2})\.(\d{1,3})\s*-->\s*(?:(\d+):)?(\d{1,2}):(\d{2})\.(\d{1,3})"""
    )
    private val STRIP_TAGS = Regex("""<[^>]+>""")

    private fun parseSrt(text: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val blocks = text.split(Regex("""\r?\n\r?\n+"""))
        for (block in blocks) {
            val lines = block.lines().filter { it.isNotBlank() }
            if (lines.isEmpty()) continue
            val timingLine = lines.firstOrNull { TIMING_SRT.containsMatchIn(it) } ?: continue
            val match = TIMING_SRT.find(timingLine) ?: continue
            val start = toMs(
                match.groupValues[1].toLong(),
                match.groupValues[2].toLong(),
                match.groupValues[3].toLong(),
                match.groupValues[4].padEnd(3, '0').take(3).toLong()
            )
            val end = toMs(
                match.groupValues[5].toLong(),
                match.groupValues[6].toLong(),
                match.groupValues[7].toLong(),
                match.groupValues[8].padEnd(3, '0').take(3).toLong()
            )
            val content = lines
                .dropWhile { !TIMING_SRT.containsMatchIn(it) }
                .drop(1)
                .joinToString("\n")
                .replace(STRIP_TAGS, "")
                .trim()
            if (content.isNotEmpty()) cues += SubtitleCue(start, end, content)
        }
        return cues.sortedBy { it.startMs }
    }

    private fun parseVtt(text: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val normalized = text.replace("\r\n", "\n")
        val blocks = normalized.split("\n\n")
        for (block in blocks) {
            val lines = block.lines().filter { it.isNotBlank() }
            if (lines.isEmpty()) continue
            if (lines.first().trim() == "WEBVTT") continue
            val timingLine = lines.firstOrNull { TIMING_VTT.containsMatchIn(it) } ?: continue
            val match = TIMING_VTT.find(timingLine) ?: continue
            val start = toMs(
                match.groupValues[1].ifEmpty { "0" }.toLong(),
                match.groupValues[2].toLong(),
                match.groupValues[3].toLong(),
                match.groupValues[4].padEnd(3, '0').take(3).toLong()
            )
            val end = toMs(
                match.groupValues[5].ifEmpty { "0" }.toLong(),
                match.groupValues[6].toLong(),
                match.groupValues[7].toLong(),
                match.groupValues[8].padEnd(3, '0').take(3).toLong()
            )
            val content = lines
                .dropWhile { !TIMING_VTT.containsMatchIn(it) }
                .drop(1)
                .joinToString("\n")
                .replace(STRIP_TAGS, "")
                .trim()
            if (content.isNotEmpty()) cues += SubtitleCue(start, end, content)
        }
        return cues.sortedBy { it.startMs }
    }

    private fun toMs(h: Long, m: Long, s: Long, ms: Long): Long =
        ((h * 3600) + (m * 60) + s) * 1000 + ms

    fun cueAt(cues: List<SubtitleCue>, positionMs: Long): SubtitleCue? {
        if (cues.isEmpty()) return null
        var lo = 0
        var hi = cues.size - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val cue = cues[mid]
            when {
                positionMs < cue.startMs -> hi = mid - 1
                positionMs > cue.endMs -> lo = mid + 1
                else -> return cue
            }
        }
        return null
    }
}
