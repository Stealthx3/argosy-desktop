package com.nendo.argosy.platform

import androidx.room.RoomDatabase

expect suspend fun <R> RoomDatabase.withTransaction(block: suspend () -> R): R
