package testUtils

//@JsName("eval")
//private external fun evalToBoolean(code: String): Boolean
//
//fun isLegacyBackend(): Boolean =
//    // Using eval to prevent DCE from thinking that following code depends on Kotlin module.
//    evalToBoolean("(typeof Kotlin != \"undefined\" && typeof Kotlin.kotlin != \"undefined\")")

@kotlin.wasm.WasmExport
fun checkOk(actualResult: String): Boolean {
    val isOk = actualResult == "OK"
    if (!isOk) {
        println("Wrong box result '${actualResult}'; Expected 'OK'")
    }
    return isOk
}