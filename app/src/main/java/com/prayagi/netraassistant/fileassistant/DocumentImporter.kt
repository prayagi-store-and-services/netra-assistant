package com.prayagi.netraassistant.fileassistant

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

/** Imports TXT and text-based PDF files into the on-device index. Scanned PDFs have no text layer and are skipped by design. */
object DocumentImporter {
    private const val CHUNK = 400

    fun displayName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) return c.getString(0)
        }
        return uri.lastPathSegment ?: "document"
    }

    /** Returns the number of pages/sections indexed, 0 when the file had no readable text. */
    fun import(context: Context, store: DocumentStore, uri: Uri): Int {
        val name = displayName(context, uri)
        val mime = context.contentResolver.getType(uri) ?: ""
        val pages: List<String> = when {
            mime == "application/pdf" || name.endsWith(".pdf", ignoreCase = true) -> extractPdfPages(context, uri)
            else -> listOf(extractText(context, uri))
        }
        store.addDocument(uri.toString(), name, pages.size)
        store.clearParagraphs(uri.toString())
        var seq = 0
        pages.forEachIndexed { pageIdx, pageText ->
            chunk(pageText).forEach { part ->
                store.insertParagraph(uri.toString(), pageIdx + 1, seq, part)
                seq++
            }
        }
        return pages.size
    }

    private fun extractText(context: Context, uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""

    private fun extractPdfPages(context: Context, uri: Uri): List<String> {
        val out = mutableListOf<String>()
        try {
            PDFBoxResourceLoader.init(context.applicationContext)
            context.contentResolver.openInputStream(uri)?.use { input ->
                val doc = PDDocument.load(input)
                try {
                    val stripper = PDFTextStripper()
                    for (p in 1..doc.numberOfPages) {
                        stripper.startPage = p
                        stripper.endPage = p
                        out.add(stripper.getText(doc))
                    }
                } finally {
                    doc.close()
                }
            }
        } catch (t: Throwable) {
            // Unreadable or encrypted PDF: skip it, nothing is uploaded anywhere.
        }
        return out
    }

    fun chunk(text: String, size: Int = CHUNK): List<String> {
        val t = text.replace(Regex("\\s+"), " ").trim()
        if (t.isEmpty()) return emptyList()
        val out = mutableListOf<String>()
        var i = 0
        while (i < t.length) {
            out.add(t.substring(i, minOf(i + size, t.length)))
            i += size
        }
        return out
    }
}
