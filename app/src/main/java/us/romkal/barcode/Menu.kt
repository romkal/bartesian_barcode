package us.romkal.barcode

import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml

@Composable
fun DropdownMenu() {
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
    DropdownMenuItem(
      text = { Text(stringResource(R.string.rate_me)) },
      onClick = {
        menuExpanded = false
        context.startActivity(
          Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
          )
        )
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