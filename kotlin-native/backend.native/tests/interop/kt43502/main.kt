import kt43502.*

fun printExternPtr() {
    @OptIn(kotlinx.cinterop.ExperimentalInterop::class)
    println(externPtr)
}