@file:KotlinOptions("-J-Xmx512m")
@file:DependsOn("log4j:log4j:1.2.14")


// instantiate a logger to test the log4j dependency
org.apache.log4j.Logger.getRootLogger()

// https://stackoverflow.com/questions/7611987/how-to-check-the-configured-xmx-value-for-a-running-java-application

//println("arg is " + args[0])
println("package_me_args_" + args.size + "_mem_" + Runtime.getRuntime().maxMemory())
