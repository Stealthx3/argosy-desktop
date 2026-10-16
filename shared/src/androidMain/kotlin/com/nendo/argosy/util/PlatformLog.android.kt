package com.nendo.argosy.util

import android.util.Log

actual object PlatformLog {
    actual fun v(tag: String?, msg: String): Int = Log.v(tag, msg)
    actual fun v(tag: String?, msg: String, tr: Throwable?): Int = Log.v(tag, msg, tr)
    actual fun d(tag: String?, msg: String): Int = Log.d(tag, msg)
    actual fun d(tag: String?, msg: String, tr: Throwable?): Int = Log.d(tag, msg, tr)
    actual fun i(tag: String?, msg: String): Int = Log.i(tag, msg)
    actual fun i(tag: String?, msg: String, tr: Throwable?): Int = Log.i(tag, msg, tr)
    actual fun w(tag: String?, msg: String): Int = Log.w(tag, msg)
    actual fun w(tag: String?, msg: String, tr: Throwable?): Int = Log.w(tag, msg, tr)
    actual fun w(tag: String?, tr: Throwable?): Int = Log.w(tag, tr)
    actual fun e(tag: String?, msg: String): Int = Log.e(tag, msg)
    actual fun e(tag: String?, msg: String, tr: Throwable?): Int = Log.e(tag, msg, tr)
}
