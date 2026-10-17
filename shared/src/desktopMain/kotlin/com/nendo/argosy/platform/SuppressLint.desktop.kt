package com.nendo.argosy.platform

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FIELD,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.LOCAL_VARIABLE,
)
@Retention(AnnotationRetention.BINARY)
actual annotation class SuppressLint(actual vararg val value: String)
