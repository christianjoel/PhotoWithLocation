package com.christianjoel.geophoto.ui.photo

import android.Manifest
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.stringResource
import com.christianjoel.geophoto.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.core.view.WindowCompat
import coil.request.ImageRequest
import com.christianjoel.geophoto.utils.captureComposeScreenshot
import com.christianjoel.geophoto.utils.saveImageToGallery
import com.christianjoel.geophoto.utils.shareImage
import com.christianjoel.geophoto.viewmodel.PhotoViewModel

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoPreviewScreen(
    viewModel: PhotoViewModel,
    onBack: () -> Unit
) {
    val captureTime by viewModel.captureTime
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()

    DisposableEffect(darkTheme) {
        val window = context.findActivity()?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            // TopAppBar is light/white, so status bar icons MUST be dark (true) in both light & dark themes
            insetsController.isAppearanceLightStatusBars = true
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
        onDispose { }
    }

    val imageUri by viewModel.imageUri
    val imageUris by viewModel.imageUris
    val urisToDisplay = imageUris.ifEmpty { listOfNotNull(imageUri) }
    val pagerState = rememberPagerState(pageCount = { urisToDisplay.size })

    val address by viewModel.address

    val isAddressReady by viewModel.isAddressFetched

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
            title = { Text(if (urisToDisplay.size > 1) stringResource(R.string.preview_photos, urisToDisplay.size) else stringResource(R.string.preview)) },
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
                        contentDescription = stringResource(R.string.delete_photo),
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
                        text = stringResource(R.string.photo_counter, pagerState.currentPage + 1, urisToDisplay.size),
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
                                imageUri = urisToDisplay.getOrNull(pagerState.currentPage) ?: urisToDisplay.firstOrNull(),
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
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            // Language Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                val currentLang = viewModel.language.value
                OutlinedButton(
                    onClick = { viewModel.setLanguage("en") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (currentLang == "en") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    )
                ) {
                    Text(stringResource(R.string.english), fontWeight = if (currentLang == "en") FontWeight.Bold else FontWeight.Normal)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { viewModel.setLanguage("ta") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (currentLang == "ta") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    )
                ) {
                    Text(stringResource(R.string.tamil), fontWeight = if (currentLang == "ta") FontWeight.Bold else FontWeight.Normal)
                }
            }

            if (!isAddressReady) {
                OutlinedButton(
                    onClick = { viewModel.retryLocation() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(if (address.contains("Fetching", true) || address.contains("பெறப்படுகிறது", true)) stringResource(R.string.fetching_location) else stringResource(R.string.retry_location))
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
                Text(stringResource(R.string.save_to_gallery))
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
                Text(stringResource(R.string.share_image))
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
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = R.mipmap.ic_launcher,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "📍 ${stringResource(R.string.watermark_location)}",
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
                text = "🕒 ${stringResource(R.string.watermark_captured)}: $captureTime",
                color = Color.White,
                fontSize = 13.sp
            )
        }
    }
}

