package kscript.app

enum class OS {
    WINDOWS, UNIX
}

fun getOS(): OS {
    val os = System.getProperty("os.name").toLowerCase()
    return when {
        os.contains("win") -> {
            OS.WINDOWS
        }
        os.contains("nix") || os.contains("nux") || os.contains("aix") ||  os.contains("mac") -> {
            OS.UNIX
        }
        else -> OS.UNIX
    }
}

val CURRENT_OS = getOS()

fun getBashCommandsForCurrentOS() = when(CURRENT_OS){
    OS.WINDOWS -> arrayOf("sh", "-c")
    OS.UNIX -> arrayOf("bash", "-c")
}