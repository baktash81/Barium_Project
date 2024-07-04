package com.example.smpp_shak_bak

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest
import android.util.Base64

class PUtil {

    companion object {
        fun encryptText(plainText: String, secretKey: String): String {
            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")

            // Generate a random initialization vector (IV)
            val iv = ByteArray(cipher.blockSize)
            val ivSpec = IvParameterSpec(iv)

            // Create the secret key from the provided key
            val secretKeySpec = SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "AES")

            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec)

            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // Encode the encrypted bytes using Base64
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        }

        fun decryptText(encryptedText: String, secretKey: String): String {
            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")

            // Generate a random initialization vector (IV)
            val iv = ByteArray(cipher.blockSize)
            val ivSpec = IvParameterSpec(iv)

            // Create the secret key from the provided key
            val secretKeySpec = SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "AES")

            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec)

            val encryptedBytes = Base64.decode(extractTextBetweenSymbols(encryptedText), Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(encryptedBytes)

            return String(decryptedBytes, Charsets.UTF_8)
        }

        private fun extractTextBetweenSymbols(input: String): String? {
            val startSymbol = "$$"
            val endSymbol = "$$"

            val startIndex = input.indexOf(startSymbol)
            val endIndex = input.lastIndexOf(endSymbol)

            if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                return input.substring(startIndex + startSymbol.length, endIndex)
            }
            return null
        }

        fun hashText(text: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(text.toByteArray())

            val result = StringBuilder()
            for (byte in digest) {
                result.append(String.format("%02x", byte))
            }

            return result.toString()
        }
    }
}
