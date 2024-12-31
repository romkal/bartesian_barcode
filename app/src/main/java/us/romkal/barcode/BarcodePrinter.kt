package us.romkal.barcode

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.print.PrintAttributes
import android.print.pdf.PrintedPdfDocument
import androidx.annotation.StringRes
import java.io.OutputStream
import kotlin.collections.component1
import kotlin.collections.component2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BarcodePrinter(private val context: Context) {
  suspend fun printPdf(
    barcode: Int,
    os: OutputStream,
    alcohols: Map<String, Float>,
    water: Float,
    glass: String,
  ) {
    return withContext(Dispatchers.IO) {
      val document = PrintedPdfDocument(
        context,
        PrintAttributes.Builder()
          .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
          .setMediaSize(PrintAttributes.MediaSize.NA_LETTER)
          .setDuplexMode(PrintAttributes.DUPLEX_MODE_NONE)
          .setMinMargins(PrintAttributes.Margins(1000, 1000, 1000, 1000))
          .build()
      )
      val page = document.startPage(0)
      val canvas = page.canvas
      val paint = Paint()
      paint.color = Color.BLACK
      canvas.translate(0f, 72f)
      canvas.save()

      repeat(5) {
        drawBarcode(barcode, canvas, paint)
        canvas.translate(0f, 72f)
      }
      canvas.restore()
      canvas.translate(100f, 0f)

      drawTextBlock(
        canvas,
        paint,
        context.getString(R.string.code, barcode),
        context.getString(R.string.glass, glass),
        *strengthDescription(R.string.standard_strength, alcohols, water),
        *strengthDescription(R.string.strong_strength, alcohols.mapValues { it.value * 0.5f }, water),
        *strengthDescription(R.string.light_strength, alcohols.mapValues { it.value * 1.5f }, water),
        *strengthDescription(R.string.mocktail_strength, emptyMap(), water + alcohols.values.sum()),
      )
      document.finishPage(page)
      document.writeTo(os)
      document.close()
    }

  }

  fun strengthDescription(
    @StringRes strengthLabelResId: Int,
    alcohols: Map<String, Float>,
    water: Float,
  ): Array<String> =
    arrayOf(
      context.getString(strengthLabelResId),
      *alcohols.map { (alcohol, amount) ->
        context.getString(
          R.string.strength_of,
          alcohol,
          amount
        )
      }.toTypedArray(),
      context.getString(R.string.water_amount, water),
      ""
    )


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