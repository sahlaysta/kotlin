// FIR_IDENTICAL

class Buildee<CT: Buildee<CT>>

fun <FT: Buildee<FT>> build(
    builder: Buildee<FT>.() -> Unit
): Buildee<FT> {
    val buildee = Buildee<FT>()
    builder(buildee)
    return buildee
}

fun test() {
    // TypeVariable(FT) is inferred into a self upper bound
    build {}
}
