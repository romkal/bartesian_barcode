package us.romkal.barcode

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.print.PrintAttributes
import android.print.pdf.PrintedPdfDocument
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BarcodePrinter(private val context: Context) {
  suspend fun printPdf(
    barcode: Int,
    os: OutputStream,
    alcohols: Map<String, Int>,
    water: Int,
    glass: String,
  ) {
    return withContext(Dispatchers.IO) {
      val document = PrintedPdfDocument(
        context, PrintAttributes.Builder()
          .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
          .setMediaSize(PrintAttributes.MediaSize.NA_LETTER)
          .setDuplexMode(PrintAttributes.DUPLEX_MODE_NONE)
          .setMinMargins(PrintAttributes.Margins(1000, 1000, 1000, 1000))
          .build()
      )
      val page = document.startPage(0)
      val canvas = page.canvas
      canvas.translate(0f, 72f)
      val paint = Paint()
      paint.color = Color.BLACK

      drawBarcode(barcode, canvas, paint)
      canvas.translate(80f, 0f)

      drawTextBlock(
        canvas,
        paint,
        context.getString(R.string.code, barcode),
        *alcohols.map { (alcohol, amount) ->
          context.getString(
            R.string.strength_of,
            alcohol,
            amount * 0.3f
          )
        }.toTypedArray(),
        context.getString(R.string.water, water / 5f),
        context.getString(R.string.glass, glass),
      )
      document.finishPage(page)
      os.use {
        document.writeTo(it)
      }
      document.close()
    }

  }

  private fun drawTextBlock(
    canvas: Canvas,
    paint: Paint,
    vararg string: String,
  ) {
    for (s in string) {
      canvas.drawText(s, 0f, 10f, paint)
      canvas.translate(0f, 16f)
    }
  }

  private fun drawBarcode(barcode: Int, canvas: Canvas, paint: Paint) {
    val unit = 36f / 68f
    paint.strokeWidth = unit * 4
    paint.style = Paint.Style.STROKE
    canvas.drawRect(0f, 0f, 72f, 36f, paint)
    paint.style = Paint.Style.FILL
    val intervals = Code128.toIntervals(barcode)
    var x = 72f / 4f
    var black = true
    for (i in intervals) {
      if (black) {
        canvas.drawRect(x, 0f, x + unit * i, 36f, paint)
      }
      black = !black
      x += unit * i
    }
  }
}