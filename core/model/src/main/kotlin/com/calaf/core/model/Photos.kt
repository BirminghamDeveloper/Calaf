package com.calaf.core.model

interface PhotoRepository {
    suspend fun slots(): List<Int>
    suspend fun read(slot: Int): ByteArray
    suspend fun upload(slot: Int, jpeg: ByteArray)
    suspend fun delete(slot: Int)
}
