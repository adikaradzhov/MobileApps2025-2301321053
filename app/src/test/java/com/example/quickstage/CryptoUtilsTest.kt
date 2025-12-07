package com.example.quickstage

import com.example.quickstage.utils.CryptoUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class CryptoUtilsTest {

    @Test
    fun generateHash_isDeterministic() {
        val id = 123
        val password = "secretPassword"
        
        val hash1 = CryptoUtils.generateHash(id, password)
        val hash2 = CryptoUtils.generateHash(id, password)
        
        assertEquals(hash1, hash2)
    }

    @Test
    fun generateHash_changesWithId() {
        val password = "secretPassword"
        
        val hash1 = CryptoUtils.generateHash(1, password)
        val hash2 = CryptoUtils.generateHash(2, password)
        
        assert(hash1 != hash2)
    }

    @Test
    fun generateHash_changesWithPassword() {
        val id = 123
        
        val hash1 = CryptoUtils.generateHash(id, "passwordA")
        val hash2 = CryptoUtils.generateHash(id, "passwordB")
        
        assert(hash1 != hash2)
    }
}
