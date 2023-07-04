// MODULE: m1-common
// FILE: common.kt
@Suppress("NO_ACTUAL_FOR_EXPECT")
@RequiresOptIn
<!EXPECT_ACTUAL_OPT_IN_ANNOTATION!>expect<!> annotation class ExpectOnly

expect annotation class ActualOnly

@RequiresOptIn
<!EXPECT_ACTUAL_OPT_IN_ANNOTATION!>expect<!> annotation class Both

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
@RequiresOptIn
<!EXPECT_ACTUAL_OPT_IN_ANNOTATION!>actual<!> annotation class ActualOnly

@RequiresOptIn
<!EXPECT_ACTUAL_OPT_IN_ANNOTATION!>actual<!> annotation class Both
