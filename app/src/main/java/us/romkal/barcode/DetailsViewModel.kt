package us.romkal.barcode

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import java.io.File
import kotlin.math.roundToInt

class DetailsViewModel(
  app: Application,
  savedStateHandle: SavedStateHandle,
) : AndroidViewModel(app) {

  private val details = savedStateHandle.toRoute<Details>()

  val scannedBarcode: Int
    get() = details.barcode

  val scannedImageFile: String?
    get() = details.file

  private fun listOfAlcohols(barcode: Int): List<Alcohol> = listOfNotNull(
    barcode.ifHasBit(11, Alcohol.TEQUILA),
    barcode.ifHasBit(10, Alcohol.VODKA),
    barcode.ifHasBit(8, Alcohol.GIN),
    barcode.ifHasBit(9, Alcohol.RUM),
    barcode.ifHasBit(12, Alcohol.WHISKEY),
  )

  fun pathForDrinkImage(drinkName: String?): String? {
    return scannedImageFile?.takeIf { scannedBarcode == barcode }
      ?: "https://romkal.github.io/bartesian_pods/images/$drinkName.jpg"
  }

  fun shareImageIntent(): Intent? {
    val sharedImageFile = File(scannedImageFile ?: return null)
    val application = getApplication<Application>()
    val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
    val emailActivity =
      application.packageManager.queryIntentActivities(emailIntent, 0).firstOrNull() ?: return null
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
      type = "message/rfc822"
      setPackage(emailActivity.activityInfo.packageName)
      putExtra(Intent.EXTRA_EMAIL, arrayOf("roman.kalukiewicz@gmail.com"))
      putExtra(Intent.EXTRA_SUBJECT, "New Bartesian Code: $scannedBarcode")
      putExtra(
        Intent.EXTRA_TEXT,
        """I've just found a new barcode for the Bartesian pod.
          |The name of the drink is:""".trimMargin()
      )
      val uriForFile =
        FileProvider.getUriForFile(application, "${application.packageName}.files", sharedImageFile)
      putExtra(Intent.EXTRA_STREAM, uriForFile)
      flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    return sendIntent
  }

  private val alcoholStates = Alcohol.entries.associateWith { alcohol ->
    val amountIdx = listOfAlcohols(scannedBarcode).indexOf(alcohol)
    val amount = if (amountIdx == -1) {
      0
    } else {
      (scannedBarcode ushr (13 + 3 * amountIdx) and 0b111) + 1
    }
    mutableIntStateOf(amount)
  }.toSortedMap()

  fun alcoholAmount(alcohol: Alcohol): State<Float> = derivedStateOf {
      val amount = alcoholStates[alcohol]!!.intValue
      if (amount == 0) 0f else amount * 0.34f - 0.20f
    }

  fun setAlcoholAmount(alcohol: Alcohol, amount: Float) {
    alcoholStates[alcohol]!!.intValue = if (amount == 0f) 0 else ((amount + 0.2f) / 0.34f).roundToInt().coerceIn(0, 8)
  }

  private var water by mutableIntStateOf(scannedBarcode ushr 3 and 0b11111)

  val waterAmount = derivedStateOf {
    amounts[water]
  }

  fun setWater(amount: Float) {
    water = waterForAmount(amount)
  }

  var glass by mutableStateOf(
    when (scannedBarcode and 0b110) {
      0b000 -> Glass.LOWBALL
      0b100 -> Glass.SHAKER
      0b010 -> Glass.HIGHBALL
      else -> throw IllegalArgumentException("Glass type unknown: $scannedBarcode")
    }
  )

  var drinkId by mutableIntStateOf(scannedBarcode ushr 22 and 0b111111111)

  val alcoholCount: Int
    get() = alcoholStates.values.filter { it.intValue > 0 }.count()

  @Stable
  val barcode: Int
    get() {
      val alcoholBits = alcoholStates.values.mapIndexed { idx, state ->
        if (state.intValue > 0) 1 shl 8 + idx else 0
      }.reduceOrNull(Int::or) ?: 0
      val sizeBits = alcoholStates.values.filter { it.intValue > 0 }.mapIndexed { idx, state ->
        (state.intValue - 1) shl 13 + (3 * idx)
      }.reduceOrNull(Int::or) ?: 0
      return 1 or when (glass) {
        Glass.HIGHBALL -> 0b010
        Glass.LOWBALL -> 0b000
        Glass.SHAKER -> 0b100
      } or alcoholBits or
        sizeBits or
        (water shl 3) or
        (drinkId shl 22)

    }

  companion object {
    fun waterForAmount(amount: Float): Int {
      val sortedAmounts = amounts.withIndex().sortedBy { it.value }
      return sortedAmounts.findLast { it.value <= amount }?.index ?: 0
    }

    val amounts = Array<Float>(1 shl 5) {
      when (it) {
        in 0..2 -> 0.25f * it
        in 3..10 -> 0.23f * it - 0.5f
        in 11..27 -> 0.44f * it - 2.65f
        in 28..30 -> 0.425f * it - 1.7f
        31 -> 12f
        else -> error("Not valid index")
      }
    }
  }
}

private fun <T> Int.ifHasBit(bit: Int, value: T): T? = if (ifHasBit(bit)) value else null

private fun Int.ifHasBit(bit: Int): Boolean = this and (1 shl bit) > 0