// FIR_IDENTICAL

class Buildee<CT>

fun <FT> build(
    builder: Buildee<FT>.() -> FT
): Buildee<FT> {
    val buildee = Buildee<FT>()
    builder(buildee)
    return buildee
}

fun test() {
    // TypeVariable(FT) is inferred into Unit
    // via builder inference from empty-lambda-as-argument's inferred Unit return type
    build {}
}
