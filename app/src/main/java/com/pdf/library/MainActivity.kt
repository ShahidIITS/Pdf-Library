package com.pdf.library

import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.pdf.mylibrary.PdfScrollView
import java.io.File

class MainActivity : AppCompatActivity() {


    private lateinit var pdfScroll: com.pdf.mylibrary.PdfScrollView

    private val pickPdfLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                pdfScroll.fromUri(it) // load PDF directly into your library
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        pdfScroll = findViewById(R.id.pdfScroll)

//        // Load from file
//        val file = File(filesDir, "sample.pdf")
//        pdfScroll.fromFile(file)
//
//        // Navigate
//        pdfScroll.scrollToPage(2)
//
//        // Zoom in
//        pdfScroll.zoom(2.0f)

        pickPdfLauncher.launch(arrayOf("application/pdf"))

    }
}