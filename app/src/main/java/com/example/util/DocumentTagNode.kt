package com.example.util

data class DocumentTagNode(
    val type: String, // "H1", "H2", "Paragraph", "List Item"
    val content: String,
    val pageNumber: Int,
    val fontSize: Float,
    val fontName: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)
