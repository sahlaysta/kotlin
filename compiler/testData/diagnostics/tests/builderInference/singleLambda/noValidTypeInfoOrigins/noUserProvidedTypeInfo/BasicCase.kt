// FIR_IDENTICAL

class Buildee<CT> // buildee type

fun <FT> build(
    builder: Buildee<FT>.() -> Unit // builder type
): Buildee<FT> {
    val buildee = Buildee<FT>()
    builder(buildee)
    return buildee
}

fun test() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> {}
}
