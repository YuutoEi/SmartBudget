package com.dhandy.smartbudget.ui.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class ThousandsSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        
        // Memformat string angka asli dengan pemisah ribuan titik
        val sb = StringBuilder()
        val len = originalText.length
        for (i in 0 until len) {
            sb.append(originalText[i])
            val remaining = len - 1 - i
            if (remaining > 0 && remaining % 3 == 0) {
                sb.append('.')
            }
        }
        
        val formattedText = sb.toString()
        
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                var dotsCount = 0
                val length = originalText.length
                val safeOffset = offset.coerceAtMost(length)
                for (i in 0 until safeOffset) {
                    val remaining = length - 1 - i
                    if (remaining > 0 && remaining % 3 == 0) {
                        dotsCount++
                    }
                }
                return safeOffset + dotsCount
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                var dotsCount = 0
                var currentTransformedIndex = 0
                val length = originalText.length
                for (i in 0 until length) {
                    currentTransformedIndex++
                    val remaining = length - 1 - i
                    if (remaining > 0 && remaining % 3 == 0) {
                        currentTransformedIndex++
                        if (currentTransformedIndex <= offset) {
                            dotsCount++
                        } else {
                            break
                        }
                    }
                }
                val originalOffset = offset - dotsCount
                return originalOffset.coerceIn(0, length)
            }
        }
        
        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}
