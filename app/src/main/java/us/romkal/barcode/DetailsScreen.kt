package us.romkal.barcode

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import us.romkal.barcode.Glass.*

@Composable
fun DetailsScreen(
  setCustomActions: (@Composable RowScope.() -> Unit) -> Unit,
  modifier: Modifier = Modifier,
) {
  val viewModel = viewModel<DetailsViewModel>()
  val barcode = viewModel.barcode
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val launcher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
      if (uri != null) {
        scope.launch {
          context.contentResolver.openOutputStream(uri)?.let {
            BarcodePrinter(context).printPdf(
              barcode,
              it,
              viewModel.alcoholStates.mapValues { it.value.intValue }.filterValues { it > 0 }
                .mapKeys { context.getString(alcoholName(it.key)) },
              viewModel.water,
              context.getString(resForGlass(viewModel.glass)),
            )
          }
        }
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf")
          .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
      }
    }
  LaunchedEffect(Unit) {
    setCustomActions {
      IconButton(
        onClick = {
          launcher.launch("barcodes.pdf")
        },
      ) {
        Icon(
          painter = painterResource(R.drawable.baseline_print_24),
          contentDescription = "Print"
        )
      }
    }
  }
  Column(
    modifier = modifier
      .imePadding()
      .padding(vertical = 8.dp, horizontal = 24.dp)
      .verticalScroll(state = rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    for (alcohol in Alcohol.entries) {
      val (amount, setAmount) = viewModel.alcoholStates[alcohol]!!
      AlcoholRow(
        alcohol,
        amount = amount,
        setAmount = setAmount,
        enabled = amount > 0 || viewModel.alcoholCount < 3
      )
    }
    GlassRow(glass = viewModel.glass, setGlass = { viewModel.glass = it })
    WaterRow(water = viewModel.water, setWater = { viewModel.water = it })
    DrinkIdRow({ viewModel.drinkId = it }, viewModel.drinkId)
    AnimatedContent(
      targetState = barcode,
      label = "Barcode",
      transitionSpec = {
        fadeIn().togetherWith(fadeOut())
      }
    ) { barcodeToShow ->
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Barcode(
          barcode = barcodeToShow, modifier = Modifier
            .widthIn(0.dp, 240.dp)
            .aspectRatio(2.0f)
            .padding(4.dp)
        )
        SelectionContainer {
          Text(
            "Code: $barcodeToShow",
            style = MaterialTheme.typography.bodyLarge
          )
        }
      }
    }
  }
}

@Composable
private fun DrinkIdRow(setDrinkId: (Int) -> Unit, drinkId: Int) {
  var enteredDrinkId by remember { mutableStateOf(drinkId.toString()) }
  fun isValid(n: String) = (n.toIntOrNull() ?: Int.MAX_VALUE) in 0..0b1111111111
  TextField(
    value = enteredDrinkId,
    onValueChange = { v ->
      enteredDrinkId = v
      if (isValid(v)) {
        setDrinkId(v.toInt())
      }
    },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    isError = !isValid(enteredDrinkId),
    singleLine = true,
    supportingText = {
      if (!isValid(enteredDrinkId)) {
        Text("Only values up to 511.")
      }
    },
    label = {Text(stringResource(R.string.drink_id))},
  )
}

@Composable
fun AlcoholRow(alcohol: Alcohol, amount: Int, setAmount: (Int) -> Unit, enabled: Boolean) {
  Column {
    Text(stringResource(alcoholName(alcohol)))
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      ) {
      Icon(contentDescription = null, painter = painterResource(alcoholSymbol(alcohol)))
      Slider(
        modifier = Modifier.weight(1f),
        value = amount.toFloat(),
        onValueChange = { setAmount(it.roundToInt()) },
        valueRange = 0f..7f,
        enabled = enabled,
      )
      Text(
        stringResource(R.string.oz, amount * 0.3f),
        modifier = Modifier.width(56.dp)
           )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlassRow(glass: Glass, setGlass: (Glass) -> Unit) {
  SingleChoiceSegmentedButtonRow {
    for ((idx, glassEntry) in Glass.entries.withIndex()) {
      SegmentedButton(
        selected = glass == glassEntry,
        onClick = { setGlass(glassEntry) },
        shape = SegmentedButtonDefaults.itemShape(idx, Glass.entries.size)
      ) {
        Text(stringResource(resForGlass(glassEntry)))
      }
    }
  }
}

@Composable
fun WaterRow(water: Int, setWater: (Int) -> Unit) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(contentDescription = null, painter = painterResource(R.drawable.water_drop_24px))
    Slider(
      modifier = Modifier.weight(1f),
      value = water.toFloat(),
      onValueChange = { setWater(it.roundToInt()) },
      valueRange = 0f..31f,
    )
    Text(
      text = stringResource(R.string.water, water / 5f)
    )
  }
}

@Composable
fun Barcode(barcode: Int, modifier: Modifier = Modifier) {
  val intervals = remember(barcode) { Code128.toIntervals(barcode) }
  Canvas(modifier = modifier) {
    drawRect(Color.White)
    var x = size.width / 4
    var unit = size.width / 2 / 68
    var black = true
    for (bar in intervals) {
      if (black) {
        drawRect(
          Color.Black,
          topLeft = Offset(x, 0f),
          size = Size(unit * bar, size.height),
        )
      }
      black = !black
      x += unit * bar
    }
    inset(unit * 1.5f) {
      drawRect(Color.Black, style = Stroke(width = 3 * unit))
    }
  }
}

@StringRes
fun alcoholName(alcohol: Alcohol) = when (alcohol) {
  Alcohol.VODKA -> R.string.vodka
  Alcohol.WHISKEY -> R.string.whiskey
  Alcohol.GIN -> R.string.gin
  Alcohol.RUM -> R.string.rum
  Alcohol.TEQUILA -> R.string.tequila
}

@DrawableRes
fun alcoholSymbol(alcohol: Alcohol) = when (alcohol) {
  Alcohol.GIN -> R.drawable.liquor_24px
  Alcohol.RUM -> R.drawable.sailing_24px
  Alcohol.VODKA -> R.drawable.water_full_24px
  Alcohol.TEQUILA -> R.drawable.local_bar_24px
  Alcohol.WHISKEY -> R.drawable.wine_bar_24px
}

@StringRes
fun resForGlass(glass: Glass) = when (glass) {
  HIGHBALL -> R.string.highball
  LOWBALL -> R.string.lowball
  SHAKER -> R.string.shaker
}