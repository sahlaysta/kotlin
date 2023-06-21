// EXPECTED_REACHABLE_NODES: 1281
package foo

private class Foo{
    data class Id(val uuid: Int)
}
private class Bar{
    data class Id(val uuid: Int)
}

private class Service{
    operator fun get(id: Foo.Id) = "Foo getter"
    operator fun get(id: Bar.Id) = "Bar getter"
}

fun box(): String {
    var service = Service()
    if (service[Bar.Id(12)] != "Bar getter") return "Fail with Bar overload"
    if (service[Foo.Id(6)] != "Foo getter") return "Fail with Foo overload"
    return "OK"
}