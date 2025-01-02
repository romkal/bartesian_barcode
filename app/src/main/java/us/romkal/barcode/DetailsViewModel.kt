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
      if (amount == 0) 0f else ALCOHOL_AMOUNTS[amount - 1]
    }

  fun setAlcoholAmount(alcohol: Alcohol, amount: Float) {
    val binarySearched = if (amount == 0f) 0 else ALCOHOL_AMOUNTS.binarySearch(amount) + 1
    val actual = if (binarySearched >= 0) binarySearched else -binarySearched - 1
    alcoholStates[alcohol]!!.intValue = actual
  }

  private var water by mutableIntStateOf(scannedBarcode ushr 3 and 0b11111)

  val waterAmount = derivedStateOf {
    WATER_AMOUNTS[water]
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
      val sortedAmounts = WATER_AMOUNTS.withIndex().sortedBy { it.value }
      return sortedAmounts.findLast { it.value <= amount }?.index ?: 0
    }

    val WATER_AMOUNTS = arrayOf(
      0f,
      0.25f,
      0.5f,
      0.15f,
      0.25f,
      0.6f,
      0.9f,
      1.1f,
      1.3f,
      1.5f,
      1.8f,
      2.2f,
      2.6f,
      3f,
      3.5f,
      4f,
      4.45f,
      4.9f,
      5.3f,
      5.75f,
      6.15f,
      6.6f,
      7f,
      7.45f,
      7.9f,
      8.4f,
      8.9f,
      9.25f,
      10.2f,
      10.65f,
      11f,
      12f,
    )

    val ALCOHOL_AMOUNTS = arrayOf(
      0.55f,
      0.8f,
      1.05f,
      1.4f,
      1.75f,
      2.15f,
      2.35f,
      3f,
    )
  }
}

private fun <T> Int.ifHasBit(bit: Int, value: T): T? = if (ifHasBit(bit)) value else null

private fun Int.ifHasBit(bit: Int): Boolean = this and (1 shl bit) > 0