package io.github.kscripting.kscript.creator

import io.github.kscripting.kscript.model.DeprecatedItem
import io.github.kscripting.kscript.model.ScriptLocation
import net.igsoft.tablevis.TableBuilder
import net.igsoft.tablevis.printer.text.TextTablePrinter
import net.igsoft.tablevis.style.text.BoxTextTableStyleSet

class DeprecatedInfoCreator {
    fun create(deprecatedItems: Set<DeprecatedItem>): String {
        val printer = TextTablePrinter()

        val deprecatedList = deprecatedItems.sortedWith(compareBy({ it.scriptLocation.level }, { it.line }))

        val table = TableBuilder(BoxTextTableStyleSet()) {
            width = 160

            row(styleSet.header) {
                cell { center(); value = "Deprecated script features report (using tablevis by aartiPl)" }
            }

            row(styleSet.header) {
                cell { id("c1"); value = "Location" }
                cell { id("c2"); value = "Line" }
                cell { id("c3"); value = "Message" }
            }

            for (deprecatedItem in deprecatedList) {

                row {
                    cell { id("c1"); value = formatLocation(deprecatedItem.scriptLocation) }
                    cell { id("c2"); value = deprecatedItem.line }
                    cell { id("c3"); value = deprecatedItem.message }
                }
            }

            forId("c1").setWidth(50)
            forId("c2").setMinimalWidth()
            forId("c3").setMinimalWidth()
        }.build()

        return printer.print(table)
    }

    private fun formatLocation(scriptLocation: ScriptLocation): String {
        return scriptLocation.sourceUri.toString()
    }
}
