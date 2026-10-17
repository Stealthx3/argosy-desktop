package com.nendo.argosy.platform

expect interface PlatformSharedPreferences {
    fun getString(key: String?, defValue: String?): String?
    fun getInt(key: String?, defValue: Int): Int
    fun getLong(key: String?, defValue: Long): Long
    fun getBoolean(key: String?, defValue: Boolean): Boolean
    fun contains(key: String?): Boolean
    fun edit(): Editor

    interface Editor {
        fun putString(key: String?, value: String?): Editor
        fun putInt(key: String?, value: Int): Editor
        fun putLong(key: String?, value: Long): Editor
        fun putBoolean(key: String?, value: Boolean): Editor
        fun remove(key: String?): Editor
        fun clear(): Editor
        fun commit(): Boolean
        fun apply()
    }
}

const val MODE_PRIVATE: Int = 0

expect fun PlatformContext.getSharedPreferences(name: String?, mode: Int): PlatformSharedPreferences
