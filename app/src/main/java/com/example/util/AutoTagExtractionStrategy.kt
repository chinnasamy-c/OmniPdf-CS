package com.example.util

import com.itextpdf.text.pdf.parser.ImageRenderInfo
import com.itextpdf.text.pdf.parser.TextRenderInfo

data class TextFragment(
    val text: String,
    val fontSize: Float,
    val fontName: String,
    val isBold: Boolean,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

class AutoTagExtractionStrategy : com.itextpdf.text.pdf.parser.TextExtractionStrategy {
    val fragments = mutableListOf<TextFragment>()
    private var currentFragment: TextFragment? = null

    override fun getResultantText(): String = ""

    override fun beginTextBlock() {}

    override fun renderText(renderInfo: TextRenderInfo?) {
        if (renderInfo == null) return
        val text = renderInfo.text
        if (text.isNullOrEmpty() || text.trim().isEmpty()) return

        val font = renderInfo.font
        val fontName = font?.postscriptFontName ?: "Unknown"
        val isBold = fontName.contains("Bold", ignoreCase = true)
        
        val bottomLeft = renderInfo.descentLine.startPoint
        val topRight = renderInfo.ascentLine.endPoint
        
        val x0 = bottomLeft.get(0)
        val y0 = bottomLeft.get(1)
        val x1 = topRight.get(0)
        val y1 = topRight.get(1)
        
        val fontSize = (y1 - y0)
        
        val newFrag = TextFragment(
            text = text,
            fontSize = fontSize,
            fontName = fontName,
            isBold = isBold,
            x = x0,
            y = y0,
            width = x1 - x0,
            height = y1 - y0
        )

        if (currentFragment != null) {
            val c = currentFragment!!
            // Check if same line and close enough
            if (Math.abs(c.y - newFrag.y) < 2f && Math.abs((c.x + c.width) - newFrag.x) < 20f && c.fontSize == newFrag.fontSize) {
                currentFragment = c.copy(
                    text = c.text + newFrag.text,
                    width = (newFrag.x + newFrag.width) - c.x
                )
            } else {
                fragments.add(c)
                currentFragment = newFrag
            }
        } else {
            currentFragment = newFrag
        }
    }

    override fun endTextBlock() {
        currentFragment?.let {
            fragments.add(it)
            currentFragment = null
        }
    }

    override fun renderImage(renderInfo: ImageRenderInfo?) {}
    
    fun flush() {
        currentFragment?.let {
            fragments.add(it)
            currentFragment = null
        }
    }
}
