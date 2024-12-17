package us.romkal.barcode

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.platform.LocalConfiguration
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(modifier: Modifier = Modifier, onBarcode: (Int) -> Unit, onNoPermission: () -> Unit) {
  val coroutineScope = rememberCoroutineScope()
  var callbackCalled by remember { mutableStateOf(false) }
  val permissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
  if (!permissionState.status.isGranted) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
      Text("Camera permission is needed to scan a barcode")
      if (permissionState.status.shouldShowRationale) {
        Button(onClick = {
          permissionState.launchPermissionRequest()
        }) {
          Text("Grant permission")
        }
      } else {
        LaunchedEffect(permissionState) {
          permissionState.launchPermissionRequest()
        }
      }
    }
    return
  }
  Box(modifier = modifier) {
    val isPortrait = LocalConfiguration.current.layoutDirection == Configuration.ORIENTATION_PORTRAIT
    Camera(remember(isPortrait) {
      Code128Analyzer(
        onBarcode = {
          coroutineScope.launch(Dispatchers.Main) {
            if (!callbackCalled) {
              onBarcode(it)
            }
            callbackCalled = true
          }
        },
        isPortrait = isPortrait,
      )
    }, modifier = Modifier.fillMaxSize())
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawRect(color = Color.Red, alpha = 0.2f)
      val windowWidth = (if (isPortrait) 0.5f else 0.6f) * size.width
      val windowHeight = windowWidth / 2f
      val rectSize = Size(width = windowWidth, height = windowHeight)
      drawRoundRect(
        topLeft = size.center - rectSize.center,
        size = rectSize,
        color = Color.Red,
        cornerRadius = CornerRadius(15f),
        style = Stroke(width = 4f)
      )
      drawRoundRect(
        topLeft = size.center - rectSize.center,
        size = rectSize,
        color = Color.Red,
        cornerRadius = CornerRadius(15f),
        blendMode = BlendMode.Clear
      )
    }
    }
}