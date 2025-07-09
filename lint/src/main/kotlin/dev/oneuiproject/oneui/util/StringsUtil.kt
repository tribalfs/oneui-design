package dev.oneuiproject.oneui.util

/**
 * Indent the string by a given number of spaces.
 *
 * @param spaces The number of spaces to indent by.
 * @param firstLineOnly Whether to indent only the first line or all lines. Defaults to true.
 * @return The indented string.
 */
fun String.indent(spaces: Int, firstLineOnly: Boolean = false): String {
    val desiredIndent = " ".repeat(spaces)
    val lines = this.lines()
    return if (firstLineOnly || lines.size == 1) {
        "$desiredIndent${this.trim()}"
    } else {
        lines.joinToString("\n") { line -> "$desiredIndent${line.trim()}" }
    }
}