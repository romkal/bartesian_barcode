package us.romkal.barcode

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import us.romkal.barcode.Glass.*
import us.romkal.barcode.ui.theme.BartesianBarcodeScannerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
  modifier: Modifier = Modifier,
  viewModel: DetailsViewModel = viewModel<DetailsViewModel>(),
  setCustomActions: (@Composable RowScope.() -> Unit) -> Unit,
) {
  val barcode = viewModel.barcode
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val drinksViewModel =
    viewModel<DrinksViewModel>(viewModelStoreOwner = context as ViewModelStoreOwner)
  val drinkMap by drinksViewModel.drinkMap.collectAsState()
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
          val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf")
            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          context.startActivity(intent)
        }
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
    val drinkName = drinkMap[barcode]
    if (drinkName != null) {
      Text(drinkName, style = MaterialTheme.typography.headlineMedium)
      AsyncImage(
        model = "https://romkal.github.io/bartesian_pods/images/$drinkName.jpg",
        contentDescription = null,
      )
    }
    ContentCard(viewModel)
    GlassRow(glass = viewModel.glass, setGlass = { viewModel.glass = it })
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
@OptIn(ExperimentalMaterial3Api::class)
private fun ContentCard(viewModel: DetailsViewModel) {
  OutlinedCard {
    val (expanded, setExpanded) = remember { mutableStateOf(false) }
    Column(
      modifier = Modifier
        .padding(8.dp)
        .fillMaxWidth()
    ) {
      CardTitle(expanded, setExpanded)
      for (alcohol in Alcohol.entries) {
        val (amount, setAmount) = viewModel.alcoholStates[alcohol]!!
        AnimatedVisibility(expanded || amount != 0) {
          AlcoholRow(
            alcohol,
            amount = amount,
            setAmount = setAmount,
            enabled = amount > 0 || viewModel.alcoholCount < 3
          )
        }
      }
      WaterRow(water = viewModel.water, setWater = { viewModel.water = it })
    }
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CardTitle(expanded: Boolean, setExpanded: (Boolean) -> Unit) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val coroutineScope = rememberCoroutineScope()
    TooltipBox(
      positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
      state = tooltipState,
      tooltip = {
        RichTooltip(
          title = { Text("About alcohol amount") },
          action = {
            Button(onClick = { tooltipState.dismiss() }) {
              Text("OK")
            }
          }
        ) {
          Text(
            """Alcohol strength provided here is for standard strength.
                  |Selecting "strong" on the machine will increase the amount of alcohol by 50%.
                  |Selecting "light" will reduce the amount by 50%.
                  |Selecting "mocktail" will add as much water as the standard amount of alcohol.
                """.trimMargin()
          )
        }
      },
    ) {
      Text(
        "Alcohol Content ❓",
        Modifier.clickable(onClick = { coroutineScope.launch { tooltipState.show() } })
      )
    }
    Spacer(Modifier.weight(1f))
    IconToggleButton(
      checked = expanded, onCheckedChange = setExpanded
    ) {
      Icon(
        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
        contentDescription = if (expanded) "Fold" else "Unfold",
      )
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
    label = { Text(stringResource(R.string.drink_id)) },
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

@Preview
@Composable
fun Preview() {
  BartesianBarcodeScannerTheme {
    DetailsScreen(viewModel = DetailsViewModel(SavedStateHandle(), scannedBarcode = 57645)) {}
  }
}