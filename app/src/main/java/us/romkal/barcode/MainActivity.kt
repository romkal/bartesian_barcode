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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
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
    val startDestination =
      if (barcodeToShow != null) Details(barcodeToShow, file = null) else Camera
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
      val snackbarHostState = remember { SnackbarHostState() }
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
                DropdownMenu()
              },
            )
          },
          snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
          NavHost(
            modifier = Modifier.padding(innerPadding),
            navController = navController,
            startDestination = startDestination
          ) {
            composable<Camera> {
              CameraScreen(
                onBarcode = { barcode, file ->
                  navController.navigate(
                    Details(
                      barcode,
                      file?.pathString
                    )
                  )
                },
                setCustomActions = setCustomActions,
              )
            }
            composable<Details> {
              DetailsScreen(
                setCustomActions = setCustomActions,
                snackbarHostState = snackbarHostState
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DropdownMenu() {
  var menuExpanded by remember { mutableStateOf(false) }
  var aboutDialogShown by remember { mutableStateOf(false) }
  if (aboutDialogShown) {
    AboutDialog(hideDialog = { aboutDialogShown = false })
  }
  IconButton(
    onClick = { menuExpanded = true }
  ) {
    Icon(imageVector = Icons.Default.MoreVert, contentDescription = stringResource(R.string.more))
  }
  DropdownMenu(
    expanded = menuExpanded,
    onDismissRequest = { menuExpanded = false },
  ) {
    val context = LocalContext.current
    DropdownMenuItem(
      text = { Text(stringResource(R.string.privacy_policy)) },
      onClick = {
        menuExpanded = false
        context.startActivity(
          Intent(
            Intent.ACTION_VIEW,
            Uri.parse(context.getString(R.string.privacy_url))
          )
        )
      }
    )
    DropdownMenuItem(
      text = { Text(stringResource(R.string.about)) },
      onClick = {
        aboutDialogShown = true
        menuExpanded = false
      }
    )
  }
}

@Composable
private fun AboutDialog(hideDialog: () -> Unit) {
  AlertDialog(
    onDismissRequest = hideDialog,
    confirmButton = {
      TextButton(
        onClick = hideDialog,
      ) {
        Text(stringResource(android.R.string.ok))
      }
    },
    icon = { Icon(painterResource(R.drawable.logo), contentDescription = null) },
    title = { Text(stringResource(R.string.about)) },
    text = {
      Text(
        AnnotatedString.fromHtml(
          stringResource(R.string.about_text).trimIndent(),
          linkStyles = TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary))
        )
      )
    }
  )
}