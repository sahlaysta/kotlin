// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
@Retention(AnnotationRetention.SOURCE)
annotation class Ann

@Ann
expect class SourceAvailable {
    @Ann
    fun foo()
}

@Ann
expect annotation class FromLib

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
class SourceAvailableImpl {
    fun foo() {}
}

actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>SourceAvailable<!> = SourceAvailableImpl

actual typealias FromLib = kotlin.SinceKotlin
