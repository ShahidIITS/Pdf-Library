package com.pdf.mylibrary

import android.graphics.Bitmap
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class PdfPageAdapter(
    private val rendererCore: PdfRendererCore,
    private val zoomProvider: () -> Float
) : RecyclerView.Adapter<PdfPageAdapter.PageVH>() {

    class PageVH(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageVH {
        val img = ImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_START
        }
        return PageVH(img)
    }

    override fun onBindViewHolder(holder: PageVH, position: Int) {
        holder.imageView.setImageBitmap(null)

        val scale = zoomProvider().coerceAtLeast(1f)

        val screenWidth = holder.itemView.resources.displayMetrics.widthPixels
        val screenHeight = holder.itemView.resources.displayMetrics.heightPixels

        // Fit to width, but cap max size
        var targetWidth = (screenWidth * scale).toInt()
        var targetHeight = (screenHeight * scale).toInt()

        // Hard limit to prevent OOM (e.g. 4096px wide, 4096px tall)
        val MAX_SIZE = 4096
        if (targetWidth > MAX_SIZE) {
            val ratio = MAX_SIZE.toFloat() / targetWidth
            targetWidth = MAX_SIZE
            targetHeight = (targetHeight * ratio).toInt()
        }
        if (targetHeight > MAX_SIZE) {
            val ratio = MAX_SIZE.toFloat() / targetHeight
            targetHeight = MAX_SIZE
            targetWidth = (targetWidth * ratio).toInt()
        }

        Thread {
            val bmp = rendererCore.renderPage(position, targetWidth, targetHeight)
            holder.imageView.post {
                if (holder.bindingAdapterPosition == position) {
                    holder.imageView.setImageBitmap(bmp)
                }
            }
        }.start()
    }


    override fun getItemCount(): Int = rendererCore.getPageCount()

    override fun onViewRecycled(holder: PageVH) {
        super.onViewRecycled(holder)
        // free bitmap reference to allow GC
        (holder.imageView.drawable)?.let {
            holder.imageView.setImageDrawable(null)
        }
    }
}
