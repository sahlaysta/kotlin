import throwThroughBridge.*

fun kotlinFun() {
    @OptIn(kotlinx.cinterop.ExperimentalInterop::class)
    throwCppException()
}