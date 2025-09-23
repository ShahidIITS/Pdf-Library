package com.pdf.mylibrary

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.github.chrisbanes.photoview.PhotoView
import kotlin.concurrent.thread

class PdfPageAdapter(
    private val context: Context,
    private val rendererCore: RendererCore,
    private val targetWidth: Int
) : RecyclerView.Adapter<PdfPageAdapter.PageViewHolder>() {

    private val bitmapCache = mutableMapOf<Int, Bitmap>()

    class PageViewHolder(val imageView: PhotoView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val imageView = PhotoView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                800 // temporary fixed height
            )
            adjustViewBounds = true
        }
        return PageViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        // Load cached bitmap if exists
        bitmapCache[position]?.let {
            holder.imageView.setImageBitmap(it)
            // adjust layout after bitmap loaded
            holder.imageView.layoutParams.height = (it.height * targetWidth / it.width)
            return
        }

        // Render bitmap in background
        thread {
            val bmp = rendererCore.renderPage(position, targetWidth)
            bitmapCache[position] = bmp
            holder.imageView.post {
                if (holder.bindingAdapterPosition == position) {
                    holder.imageView.setImageBitmap(bmp)
                    // adjust layout
                    holder.imageView.layoutParams.height = (bmp.height * targetWidth / bmp.width)
                    holder.imageView.requestLayout()
                }
            }
        }
    }

    override fun getItemCount(): Int = rendererCore.getPageCount()
}

