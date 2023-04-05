@file:DependsOn("com.offbytwo:docopt:0.6.0.20150202")
@file:DependsOn("log4j:log4j:1.2.14")
@file:Import("multi1.main.kts")
@file:DependsOn("com.jakewharton.picnic:picnic:0.5.0")
@file:DependsOn("net.igsoft:tablevis:0.6.0")

import org.docopt.Docopt
import com.jakewharton.picnic.*
import net.igsoft.tablevis.TableBuilder
import net.igsoft.tablevis.printer.text.TextTablePrinter
import net.igsoft.tablevis.style.text.BoxTextTableStyleSet

// test the docopt dependency
val docopt = Docopt("Usage: jl <command> [options] [<joblist_file>]")

// instantiate a logger to test the log4j dependency
org.apache.log4j.Logger.getRootLogger()

println("kscript is  cool!")
println("${args.size}: " + args.joinToString(", ", "[", "]"))


val printer = TextTablePrinter()

var tableVis = TableBuilder(BoxTextTableStyleSet()) {
    width = 100
    center()

    row(styleSet.header) {
        cell {
            value = "Release name"
        }

        cell {
            value = "Author"
        }

        cell {
            value = "Download count"
        }

        cell {
            value = "Publish date"
        }
    }

    row {
        cell {
            value = "r1c1"
        }

        cell {
            value = "r1c2"
        }

        cell {
            value = "r1c3"
        }

        cell {
            value = "r1c4"
        }
    }

    row {
        cell {
            value = "r2c1"
        }

        cell {
            value = "r2c2"
        }

        cell {
            value = "r2c3"
        }

        cell {
            value = "r2c4"
        }
    }
}.build()


println(tableVis)
println(printer.print(tableVis))


val tableToPrint = table {
    cellStyle {
        border = true
    }
    row {
        cell("Hello") {
            rowSpan = 2
        }
        cell("World")
    }
    // This row has only one cell because "Hello" will carry over and push it to the right.
    row("Mars")

    // This row has only one cell because it spans two columns.
    row {
        cell("Hola Mundo") {
            columnSpan = 2
        }
    }
}


println(tableToPrint)


Test1().hello()
