//KOTLIN_ARGS -J-Dkotlin.main.kts.jvm.target=11
//KOTLIN_VERSION = 2.2.1-RC
data class Message(val text: String)
fun createMessage(name: String): Message = Message("Hello ${'$'}name from Kotlin 2.2.1-RC!")
val message = createMessage("kscript")
println(message.text)
