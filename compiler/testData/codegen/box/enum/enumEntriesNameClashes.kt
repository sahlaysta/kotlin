// !LANGUAGE: +EnumEntries
// KT-59611
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, NATIVE, WASM
// IGNORE_BACKEND: JS, JVM
// WITH_STDLIB

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

import kotlin.enums.*


enum class EnumWithClash {
    values,
    entries,
    valueOf;
}

@OptIn(ExperimentalStdlibApi::class)
fun box(): String {
    val ref = EnumWithClash::entries
    if (ref().toString() != "[values, entries, valueOf]") return "FAIL 1"
    if (EnumWithClash.entries.toString() != "entries") return "FAIL 2"
    if (enumEntries<EnumWithClash>().toString() != "[values, entries, valueOf]") return "FAIL 3"
    return "OK"
}