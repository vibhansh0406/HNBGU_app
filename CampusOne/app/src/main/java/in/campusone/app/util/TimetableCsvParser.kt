package `in`.campusone.app.util

import java.util.Locale

/**
 * Parses a subject-based timetable CSV.
 * Required columns (case-insensitive, order flexible):
 * subject_code, subject_name, room, day, start, end
 * Day: 1=Mon .. 7=Sun. Time: HH:mm (24h).
 * Extra legacy columns are ignored.
 */
object TimetableCsvParser {

    data class ParsedRow(
        val subjectCode: String,
        val subjectName: String,
        val roomNumber: String,
        val dayOfWeek: Int,
        val startTime: String,
        val endTime: String
    ) {
        fun toRemoteMap(): Map<String, Any> = mapOf(
            "subject_code" to subjectCode,
            "subject_name" to subjectName,
            "room_number" to roomNumber,
            "day_of_week" to dayOfWeek,
            "start_time" to startTime,
            "end_time" to endTime,
            "is_elective" to false
        )
    }

    data class ParseResult(
        val rows: List<ParsedRow>,
        val errors: List<String>
    )

    private val REQUIRED_HEADERS = setOf("subject_code", "subject_name", "room", "day", "start", "end")
    private val TIME_REGEX = "^([01]\\d|2[0-3]):[0-5]\\d$".toRegex()

    fun parse(csvText: String): ParseResult {
        val lines = csvText.trim().split("\n").filter { it.isNotBlank() }
        if (lines.isEmpty()) return ParseResult(emptyList(), listOf("Empty file"))

        val headers = lines[0].toLowerCase(Locale.ROOT).split(",").map { it.trim() }
        val headerIndex = headers.mapIndexed { i, h -> h to i }.toMap()
        val missing = REQUIRED_HEADERS - headerIndex.keys
        if (missing.isNotEmpty()) {
            return ParseResult(emptyList(), listOf("Missing required columns: ${missing.joinToString(", ")}"))
        }

        val rows = mutableListOf<ParsedRow>()
        val errors = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        lines.drop(1).forEachIndexed { rowIdx, line ->
            val lineNum = rowIdx + 2
            val cols = line.split(",").map { it.trim() }
            if (cols.size < headers.size) {
                errors.add("Line $lineNum: not enough columns")
                return@forEachIndexed
            }

            val get = { key: String -> cols[headerIndex[key]!!] }
            val subjectCode = get("subject_code")
            val subjectName = get("subject_name")
            val room = get("room")
            val dayStr = get("day")
            val start = get("start")
            val end = get("end")

            if (subjectCode.isBlank() || subjectName.isBlank() || room.isBlank() ||
                dayStr.isBlank() || start.isBlank() || end.isBlank()) {
                errors.add("Line $lineNum: empty required field")
                return@forEachIndexed
            }

            val day = dayStr.toIntOrNull()
            if (day == null || day !in 1..7) {
                errors.add("Line $lineNum: day must be 1-7")
                return@forEachIndexed
            }
            if (!TIME_REGEX.matches(start) || !TIME_REGEX.matches(end)) {
                errors.add("Line $lineNum: time must be HH:mm (24h)")
                return@forEachIndexed
            }
            if (timeToMinutes(end) <= timeToMinutes(start)) {
                errors.add("Line $lineNum: end time must be after start time")
                return@forEachIndexed
            }

            val dupKey = "$subjectCode|$day|$start"
            if (dupKey in seen) {
                errors.add("Line $lineNum: duplicate slot for same subject/day/start")
                return@forEachIndexed
            }
            seen.add(dupKey)
            rows.add(ParsedRow(subjectCode, subjectName, room, day, start, end))
        }
        return ParseResult(rows, errors)
    }

    private fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    fun template(): String =
        "subject_code,subject_name,room,day,start,end\n" +
        "SOET-CS-301,Data Structures & Algorithms,CS Block - Room 104,1,09:00,10:00\n" +
        "SOET-CS-302,Computer Organization,CS Block - Room 105,1,10:15,11:15\n" +
        "SOET-CS-303,Discrete Mathematics,CS Block - Room 106,2,09:00,10:00\n"
}
