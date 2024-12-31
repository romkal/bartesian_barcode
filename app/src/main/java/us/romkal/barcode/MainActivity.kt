package us.romkal.barcode

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlin.io.path.pathString
import us.romkal.barcode.ui.theme.BartesianBarcodeScannerTheme

class MainActivity : ComponentActivity() {
  @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val barcodeToShow = intent.data?.schemeSpecificPart?.toIntOrNull()
    val startDestination = if (barcodeToShow != null) Details(barcodeToShow, file = null) else Camera
    enableEdgeToEdge()
    setContent {
      val drinksViewModel = viewModel<DrinksViewModel>()
      LaunchedEffect(Unit) {
        drinksViewModel.loadDrinksFromNetwork()
      }
      val navController = rememberNavController()
      val (customActions, setCustomActions) = remember {
        mutableStateOf<@Composable RowScope.() -> Unit>({})
      }
      BartesianBarcodeScannerTheme {
        Scaffold(
          modifier = Modifier
            .fillMaxSize()
            .consumeWindowInsets(WindowInsets.navigationBars),
          topBar = {
            TopAppBar(
              title = { Text(stringResource(R.string.app_name)) },
              navigationIcon = {
                IconButton(onClick = { if (!navController.popBackStack()) finish() }) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(
                      R.string.navigate_back
                    )
                  )
                }
              },
              actions = {
                customActions()
                PrivacyPolicy()
              },
            )
          },
        ) { innerPadding ->
          NavHost(
            modifier = Modifier.padding(innerPadding),
            navController = navController,
            startDestination = startDestination
          ) {
            composable<Camera> {
              CameraScreen(
                onBarcode = { barcode, file -> navController.navigate(Details(barcode, file?.pathString)) },
                setCustomActions = setCustomActions,
              )
            }
            composable<Details> { DetailsScreen(setCustomActions = setCustomActions) }
          }
        }
      }
    }
  }
}

@Composable
private fun PrivacyPolicy() {
  var expanded by remember { mutableStateOf(false) }
  IconButton(
    onClick = { expanded = true }
  ) {
    Icon(imageVector = Icons.Default.MoreVert, contentDescription = stringResource(R.string.more))
  }
  DropdownMenu(
    expanded = expanded,
    onDismissRequest = { expanded = false },
  ) {
    val context = LocalContext.current
    DropdownMenuItem(
      text = { Text(stringResource(R.string.privacy_policy)) },
      onClick = {
        context.startActivity(
          Intent(
            Intent.ACTION_VIEW,
            Uri.parse(context.getString(R.string.privacy_url))
          )
        )
      }
    )
  }
}
