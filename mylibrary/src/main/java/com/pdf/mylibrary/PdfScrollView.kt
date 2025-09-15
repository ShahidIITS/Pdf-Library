package com.pdf.mylibrary

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class PdfScrollView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var rendererCore: PdfRendererCore? = null
    private val recyclerView = RecyclerView(context)
    private var adapter: PdfPageAdapter? = null

    // zoom factor
    private var scaleFactor = 1f

    private val handler = Handler(Looper.getMainLooper())
    private var pendingRerender: Runnable? = null
    private val rerenderDelayMs = 120L // small debounce while user is pinching

    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                // stop parent from intercepting while scaling
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val prev = scaleFactor
                scaleFactor *= detector.scaleFactor
                scaleFactor = scaleFactor.coerceIn(1f, 5f)
                if (prev != scaleFactor) scheduleRerenderVisiblePages()
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                parent?.requestDisallowInterceptTouchEvent(false)
                // final rerender
                scheduleRerenderVisiblePages(immediate = true)
            }
        })

    init {
        recyclerView.layoutManager = LinearLayoutManager(context)
        addView(recyclerView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        // Make sure pinch events reach the detector even though RecyclerView scrolls.
        recyclerView.setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            // return false so RecyclerView still receives touch events for scrolling
            false
        }
    }

    private fun scheduleRerenderVisiblePages(immediate: Boolean = false) {
        pendingRerender?.let { handler.removeCallbacks(it) }
        val runnable = Runnable { rerenderVisiblePages() }
        pendingRerender = runnable
        if (immediate) handler.post(runnable) else handler.postDelayed(runnable, rerenderDelayMs)
    }

    private fun rerenderVisiblePages() {
        val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val first = lm.findFirstVisibleItemPosition().coerceAtLeast(0)
        val last = lm.findLastVisibleItemPosition().coerceAtLeast(first)
        if (first <= last) {
            adapter?.let {
                // notify only visible range so only those get re-bound (and re-rendered)
                it.notifyItemRangeChanged(first, last - first + 1)
            }
        }
    }

    fun fromFile(file: File) {
        close()
        rendererCore = PdfRendererCore(file)
        setupAdapter()
    }

    fun fromUri(uri: Uri) {
        val file = File(context.cacheDir, "temp.pdf")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        fromFile(file)
    }

    fun fromBytes(bytes: ByteArray) {
        val file = File(context.cacheDir, "temp.pdf")
        file.writeBytes(bytes)
        fromFile(file)
    }

    fun fromAsset(assetName: String) {
        val file = File(context.cacheDir, "asset.pdf")
        context.assets.open(assetName).use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        fromFile(file)
    }

    private fun setupAdapter() {
        rendererCore?.let {
            adapter = PdfPageAdapter(it) { scaleFactor }
            recyclerView.adapter = adapter
        }
    }

    fun getPageCount(): Int = rendererCore?.getPageCount() ?: 0

    fun scrollToPage(index: Int) {
        recyclerView.scrollToPosition(index)
    }

    fun zoom(scale: Float) {
        scaleFactor = scale.coerceIn(1f, 5f)
        scheduleRerenderVisiblePages(immediate = true)
    }

    fun close() {
        rendererCore?.close()
        rendererCore = null
        adapter = null
        recyclerView.adapter = null
    }

    // ensure ScaleGestureDetector is fed if touch somehow reaches parent (just in case)
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }
}
