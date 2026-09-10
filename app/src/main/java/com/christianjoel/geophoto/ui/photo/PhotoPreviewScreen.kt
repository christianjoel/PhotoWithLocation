package com.christianjoel.geophoto.ui.photo

import android.Manifest
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
    val captureTime by viewModel.captureTime
    val context = LocalContext.current

    val imageUri by viewModel.imageUri
    val imageUris by viewModel.imageUris
    val urisToDisplay = imageUris.ifEmpty { listOfNotNull(imageUri) }
    val pagerState = rememberPagerState(pageCount = { urisToDisplay.size })

    val address by viewModel.address

    val isAddressReady = address.isNotBlank() &&
            !address.contains("Fetching", true) &&
            !address.contains("not available", true) &&
            !address.contains("Unable", true) &&
            !address.contains("Unknown", true)

    var captureView by remember { mutableStateOf<View?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            captureView?.let {
                val file = captureComposeScreenshot(context, it)
                saveImageToGallery(context, file)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // 🔹 TOP BAR
        TopAppBar(
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            title = { Text(if (urisToDisplay.size > 1) "Preview (${urisToDisplay.size} Photos)" else "Preview") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBackIosNew, null)
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        val currentUri = urisToDisplay.getOrNull(pagerState.currentPage)
                        if (currentUri != null) {
                            viewModel.removeImage(currentUri)
                            if (urisToDisplay.size <= 1) {
                                onBack()
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Photo",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        )

        // 🔹 CAPTURE-ONLY CONTENT
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (urisToDisplay.size > 1) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val uri = urisToDisplay.getOrNull(page)
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            ComposeView(ctx).also { view ->
                                if (page == pagerState.currentPage) captureView = view
                            }
                        },
                        update = { view ->
                            if (page == pagerState.currentPage) captureView = view
                            view.setContent {
                                CaptureOnlyContent(
                                    imageUri = uri,
                                    address = address,
                                    captureTime = captureTime
                                )
                            }
                        }
                    )
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Photo ${pagerState.currentPage + 1} of ${urisToDisplay.size}",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        ComposeView(ctx).also { captureView = it }
                    },
                    update = { view ->
                        captureView = view
                        view.setContent {
                            CaptureOnlyContent(
                                imageUri = urisToDisplay.firstOrNull(),
                                address = address,
                                captureTime = captureTime
                            )
                        }
                    }
                )
            }
        }

        // 🔹 ACTION BUTTONS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (!isAddressReady) {
                OutlinedButton(
                    onClick = { viewModel.retryLocation() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(if (address.contains("Fetching", true)) "Fetching Location..." else "Retry Location")
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                enabled = isAddressReady,
                onClick = {
                    captureView?.let {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val file = captureComposeScreenshot(context, it)
                            saveImageToGallery(context, file)
                        } else {
                            // Android 9 and below — need permission
                            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
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
    address: String,
    captureTime: String
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
            Text(
                text = "🕒 $captureTime",
                color = Color.White,
                fontSize = 13.sp
            )
        }
    }
}

