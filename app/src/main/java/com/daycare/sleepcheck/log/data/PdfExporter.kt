package com.daycare.sleepcheck.log.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.text.DateFormat
import java.util.Date

class PdfExporter(private val context: Context, private val resolver: ContentResolver) {
    fun write(
        uri: Uri,
        facility: FacilityEntity?,
        rooms: List<RoomEntity>,
        staff: List<StaffEntity>,
        records: List<CheckRecordEntity>,
    ) {
        val document = PdfDocument()
        val paint = Paint().apply { textSize = 12f; color = android.graphics.Color.BLACK }
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true; color = android.graphics.Color.BLACK }
        val dateTime = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(612, 792, pageNumber).create())
        var y = 48f
        fun newPage() {
            document.finishPage(page)
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(612, 792, pageNumber).create())
            y = 48f
        }
        fun drawWrapped(text: String, textPaint: Paint = paint) {
            var remaining = text
            do {
                if (y > 740f) newPage()
                val count = textPaint.breakText(remaining, true, 532f, null).coerceAtLeast(1)
                page.canvas.drawText(remaining.take(count), 40f, y, textPaint)
                remaining = remaining.drop(count).trimStart()
                y += if (textPaint === titlePaint) 26f else 18f
            } while (remaining.isNotEmpty())
        }

        drawWrapped(context.getString(com.daycare.sleepcheck.log.R.string.pdf_title), titlePaint)
        y += 8f
        facility?.let {
            drawWrapped(context.getString(com.daycare.sleepcheck.log.R.string.pdf_facility, it.name))
            y += 4f
        }
        if (records.isEmpty()) drawWrapped(context.getString(com.daycare.sleepcheck.log.R.string.pdf_no_records))
        records.forEach { record ->
            if (y > 700f) newPage()
            val roomName = rooms.firstOrNull { it.id == record.roomId }?.name ?: record.roomId
            val staffName = staff.firstOrNull { it.id == record.staffId }?.name ?: record.staffId
            val observation = context.getString(if (record.observationType == ObservationType.EXCEPTION.name) com.daycare.sleepcheck.log.R.string.pdf_exception else com.daycare.sleepcheck.log.R.string.pdf_normal)
            val status = context.getString(if (record.isLate) com.daycare.sleepcheck.log.R.string.pdf_late else com.daycare.sleepcheck.log.R.string.pdf_on_time)
            drawWrapped(context.getString(com.daycare.sleepcheck.log.R.string.pdf_room, roomName))
            drawWrapped(context.getString(com.daycare.sleepcheck.log.R.string.pdf_staff, staffName))
            drawWrapped(context.getString(com.daycare.sleepcheck.log.R.string.pdf_status, observation, status))
            drawWrapped(context.getString(com.daycare.sleepcheck.log.R.string.pdf_scheduled_time, dateTime.format(Date(record.scheduledAt))))
            drawWrapped(context.getString(com.daycare.sleepcheck.log.R.string.pdf_observed_time, dateTime.format(Date(record.observedAt))))
            drawWrapped(context.getString(com.daycare.sleepcheck.log.R.string.pdf_recorded_time, dateTime.format(Date(record.recordedAt))))
            if (record.notes.isNotBlank()) drawWrapped(context.getString(com.daycare.sleepcheck.log.R.string.pdf_notes, record.notes))
            y += 10f
        }
        document.finishPage(page)
        resolver.openOutputStream(uri)?.use { document.writeTo(it) } ?: error("Unable to open PDF destination")
        document.close()
    }
}
