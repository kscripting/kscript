@file:DependsOn("com.offbytwo:docopt:0.6.0.20150202")
@file:DependsOn("log4j:log4j:1.2.14")
@file:Import("multi1.main.kts")
@file:DependsOn("com.jakewharton.picnic:picnic:0.5.0")

import org.docopt.Docopt
import com.jakewharton.picnic.*

// test the docopt dependency
val docopt = Docopt("Usage: jl <command> [options] [<joblist_file>]")

// instantiate a logger to test the log4j dependency
org.apache.log4j.Logger.getRootLogger()

println("kscript is  cool!")
println("${args.size}: " + args.joinToString(", ", "[", "]"))


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
