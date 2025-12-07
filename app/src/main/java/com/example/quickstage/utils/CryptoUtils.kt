package com.example.quickstage.utils

import java.security.MessageDigest

object CryptoUtils {
    fun generateHash(ticketId: Int, adminPassword: String): String {
        val input = "$ticketId:$adminPassword"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
