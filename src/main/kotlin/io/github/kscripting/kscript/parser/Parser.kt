package io.github.kscripting.kscript.parser

import io.github.kscripting.kscript.model.Code
import io.github.kscripting.kscript.model.Location
import io.github.kscripting.kscript.model.Section

class Parser {
    private val annotationParsers = listOf(
        LineParser::parseSheBang,
        LineParser::parseInclude,
        LineParser::parseDependency,
        LineParser::parseRepository,
        LineParser::parseEntry,
        LineParser::parseKotlinOpts,
        LineParser::parseCompilerOpts,
        LineParser::parseImport,
        LineParser::parsePackage,
    )

    fun parse(location: Location, string: String): List<Section> {
        val codeTextAsLines = string.lines()

        val sections = mutableListOf<Section>()

        for (line in codeTextAsLines.withIndex()) {
            val section = parseLine(location, line.index + 1, line.value)
            sections += section
        }
        return sections
    }

    private fun parseLine(location: Location, line: Int, text: String): Section {
        for (parser in annotationParsers) {
            val parsedAnnotations = parser(location, line, text)

            if (parsedAnnotations.isNotEmpty()) {
                return Section(text, parsedAnnotations)
            }
        }
        return Section(text, listOf(Code))
    }
}
