package com.pdf.mylibrary

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor

class RendererCore(private val fileDescriptor: ParcelFileDescriptor) {

    private val pdfRenderer = PdfRenderer(fileDescriptor)

    fun getPageCount(): Int = pdfRenderer.pageCount

    fun renderPage(pageIndex: Int, targetWidth: Int, scaleFactor: Float =3f): Bitmap {
        pdfRenderer.openPage(pageIndex).use { page ->
            // Calculate scaled width and height
            val width = (targetWidth * 3f).toInt()  // 3x for high DPI

//            val width = (targetWidth * scaleFactor).toInt().coerceAtLeast(1)
            val ratio = page.height.toFloat() / page.width.toFloat()
            val height = (width * ratio).toInt().coerceAtLeast(1)

            // Create high-quality bitmap
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.density = android.util.DisplayMetrics.DENSITY_DEFAULT

            // Render page
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        }
    }

    fun close() {
        pdfRenderer.close()
        fileDescriptor.close()
    }
}
