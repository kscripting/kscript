@file:KotlinOpts("-J-Xmx5g   -Dkscript.opt1=1")
//KOTLIN_OPTS  -Dkscript.opt2=foo   -Dkscript.opt3=bar

// https://stackoverflow.com/questions/7611987/how-to-check-the-configured-xmx-value-for-a-running-java-application

//println("arg is " + args[0])
println("mem_${Runtime.getRuntime().maxMemory()}_${System.getProperty("kscript.opt1")}_${System.getProperty("kscript.opt2")}_${System.getProperty("kscript.opt3")}")