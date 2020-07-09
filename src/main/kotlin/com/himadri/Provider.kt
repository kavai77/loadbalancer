package com.himadri

interface Provider {
    fun get(): String
    suspend fun check()
}