package com.pdf.mylibrary


import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

class PdfDocumentView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var rendererCore: PdfRendererCore? = null
    private val pdfView = PdfView(context)

    init {
        addView(pdfView)
    }

    fun fromFile(file: java.io.File) {
        rendererCore?.close()
        rendererCore = PdfRendererCore(file)
        showPage(0) // show first page
    }

    fun showPage(index: Int) {
        rendererCore?.let {
            val bmp = it.renderPage(index, width.takeIf { it > 0 } ?: 1080, height.takeIf { it > 0 } ?: 1920)
            pdfView.showPage(bmp)
        }
    }

    fun getPageCount(): Int = rendererCore?.getPageCount() ?: 0

    fun close() {
        rendererCore?.close()
    }
}
