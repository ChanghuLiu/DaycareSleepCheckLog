package com.daycare.sleepcheck.log.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.text.DateFormat
import java.util.Date

class PdfExporter(private val context: Context, private val resolver: ContentResolver) {
    fun write(uri: Uri, records: List<CheckRecordEntity>) {
        val document = PdfDocument()
        val paint = Paint().apply { textSize = 12f; color = android.graphics.Color.BLACK }
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true; color = android.graphics.Color.BLACK }
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(612, 792, pageNumber).create())
        var y = 48f
        page.canvas.drawText(context.getString(com.daycare.sleepcheck.log.R.string.pdf_title), 40f, y, titlePaint)
        y += 32f
        records.forEach { record ->
            if (y > 740f) { document.finishPage(page); pageNumber += 1; page = document.startPage(PdfDocument.PageInfo.Builder(612, 792, pageNumber).create()); y = 48f }
            val observation = context.getString(if (record.observationType == ObservationType.EXCEPTION.name) com.daycare.sleepcheck.log.R.string.pdf_exception else com.daycare.sleepcheck.log.R.string.pdf_normal)
            val line = "${DateFormat.getDateTimeInstance().format(Date(record.recordedAt))}  $observation  ${context.getString(if (record.isLate) com.daycare.sleepcheck.log.R.string.pdf_late else com.daycare.sleepcheck.log.R.string.pdf_on_time)}"
            page.canvas.drawText(line, 40f, y, paint); y += 20f
            page.canvas.drawText(context.getString(com.daycare.sleepcheck.log.R.string.pdf_scheduled, record.scheduledAt, record.observedAt, record.notes), 40f, y, paint); y += 28f
        }
        document.finishPage(page)
        resolver.openOutputStream(uri)?.use { document.writeTo(it) } ?: error("Unable to open PDF destination")
        document.close()
    }
}
