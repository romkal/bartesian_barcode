package us.romkal.barcode

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

class DetailsViewModel(
  savedStateHandle: SavedStateHandle): ViewModel() {
  val scannedBarcode = savedStateHandle.toRoute<Details>().barcode

    private fun listOfAlcohols(barcode: Int): List<Alcohol> = listOfNotNull(
    barcode.ifHasBit(11, Alcohol.TEQUILA),
    barcode.ifHasBit(10, Alcohol.VODKA),
    barcode.ifHasBit(8, Alcohol.GIN),
    barcode.ifHasBit(9, Alcohol.RUM),
    barcode.ifHasBit(12, Alcohol.WHISKEY),
  )

  val alcoholStates = Alcohol.entries.associateWith { alcohol ->
    val amountIdx = listOfAlcohols(scannedBarcode).indexOf(alcohol)
    val amount = if (amountIdx == -1) {
      0
    } else {
      scannedBarcode ushr (13 + 3 * amountIdx) and 0b111
    }
    mutableIntStateOf(amount)
  }.toSortedMap()

  var water by mutableIntStateOf(scannedBarcode ushr 3 and 0b11111)

  var glass by mutableStateOf(when(scannedBarcode and 0b110) {
    0b000 -> Glass.LOWBALL
    0b100 -> Glass.SHAKER
    0b010 -> Glass.HIGHBALL
    else -> throw IllegalArgumentException("Glass type unknown: $scannedBarcode")
  })

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
        state.intValue shl 13 + (3*idx)
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

}

private fun <T> Int.ifHasBit(bit: Int, value: T): T? = if (ifHasBit(bit)) value else null



private fun Int.ifHasBit(bit: Int): Boolean = this and (1 shl bit) > 0