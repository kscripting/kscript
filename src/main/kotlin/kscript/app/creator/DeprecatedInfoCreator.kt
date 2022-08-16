package kscript.app.creator

import kscript.app.model.Deprecated
import kscript.app.model.Location
import net.igsoft.tablevis.TableBuilder
import net.igsoft.tablevis.printer.text.TextTablePrinter
import net.igsoft.tablevis.style.text.BoxTextTableStyleSet

class DeprecatedInfoCreator {
    fun create(deprecated: Set<Deprecated>): String {
        val printer = TextTablePrinter()

        val deprecatedList = deprecated.sortedWith(compareBy({ it.location.level }, { it.line }))

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
                    cell { id("c1"); value = formatLocation(deprecatedItem.location) }
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

    private fun formatLocation(location: Location): String {
        return location.sourceUri.toString()
    }
}
