// FIR_IDENTICAL

interface UpperBound
class Buildee<CT: UpperBound>

fun <FT: UpperBound> build(
    builder: Buildee<FT>.() -> Unit
): Buildee<FT> {
    val buildee = Buildee<FT>()
    builder(buildee)
    return buildee
}

fun test() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> {}
}
