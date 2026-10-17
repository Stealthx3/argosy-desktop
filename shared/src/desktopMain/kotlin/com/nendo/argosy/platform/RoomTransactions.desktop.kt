package com.nendo.argosy.platform

import androidx.room.RoomDatabase
import androidx.room.Transactor
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection

actual suspend fun <R> RoomDatabase.withTransaction(block: suspend () -> R): R =
    useWriterConnection { transactor -> transactor.immediateTransaction { block() } }
