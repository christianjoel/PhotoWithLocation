package com.christianjoel.geophoto.ui.photo

import android.net.Uri
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.christianjoel.geophoto.utils.captureComposeScreenshot
import com.christianjoel.geophoto.utils.saveImageToGallery
import com.christianjoel.geophoto.utils.shareImage
import com.christianjoel.geophoto.viewmodel.PhotoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoPreviewScreen(
    viewModel: PhotoViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val imageUri by viewModel.imageUri
    val address by viewModel.address

    val isAddressReady = address.isNotBlank() &&
            !address.contains("Fetching", true) &&
            !address.contains("Unable", true)

    var captureView by remember { mutableStateOf<View?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {

        // 🔹 TOP BAR
        TopAppBar(
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            title = { Text("Preview") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBackIosNew, null)
                }
            }
        )

        // 🔹 CAPTURE-ONLY CONTENT
        AndroidView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            factory = { ctx ->
                ComposeView(ctx).also { captureView = it }
            },
            update = { view ->
                view.setContent {
                    CaptureOnlyContent(
                        imageUri = imageUri,
                        address = address
                    )
                }
            }
        )

        // 🔹 ACTION BUTTONS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Button(
                enabled = isAddressReady,
                onClick = {
                    captureView?.let {
                        val file = captureComposeScreenshot(context, it)
                        saveImageToGallery(context, file)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Save to Gallery")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                enabled = isAddressReady,
                onClick = {
                    captureView?.let {
                        val file = captureComposeScreenshot(context, it)
                        shareImage(context, file)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Share Image with Location")
            }
        }
    }
}


@Composable
private fun CaptureOnlyContent(
    imageUri: Uri?,
    address: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        imageUri?.let { uri ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(uri)
                    .allowHardware(false)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black)
                    )
                )
                .padding(16.dp)
        ) {
            Text(
                text = "📍 Location",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = address,
                color = Color.White,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

