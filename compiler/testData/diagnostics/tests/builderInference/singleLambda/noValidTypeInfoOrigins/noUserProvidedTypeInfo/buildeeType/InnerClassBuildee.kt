// FIR_IDENTICAL

class OuterKlass<CT> {
    inner class Buildee
}

fun <FT> build(
    builder: OuterKlass<FT>.Buildee.() -> Unit
): OuterKlass<FT>.Buildee {
    val buildee = OuterKlass<FT>().Buildee()
    builder(buildee)
    return buildee
}

fun test() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> {}
}
