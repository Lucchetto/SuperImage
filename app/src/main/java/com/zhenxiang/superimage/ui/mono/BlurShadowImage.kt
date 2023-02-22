package com.zhenxiang.superimage.ui.mono

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.*
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.transition.CrossfadeTransition
import com.zhenxiang.superimage.ui.dpToPx
import com.zhenxiang.superimage.ui.pxToDp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun BlurShadowImage(
    model: ImageRequest,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
) = Box(
    modifier = modifier,
    contentAlignment = Alignment.Center
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val blurShadowProvider = remember { BlurShadowProvider(context) }
    var blurShadowBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val blurRadius = 70.dp.dpToPx()

    SubcomposeAsyncImage(
        model = model,
        contentDescription = contentDescription,
        success = {
            BlurShadow(blurShadowBitmap)
            SubcomposeAsyncImageContent(modifier = imageModifier)
        },
        onLoading = { blurShadowBitmap = null },
        onSuccess = {
            coroutineScope.launch(Dispatchers.IO) {
                (it.result.drawable as? BitmapDrawable)?.bitmap?.let { bitmap ->
                    blurShadowProvider.getBlurShadow(bitmap, blurRadius)?.let { blur ->
                        blurShadowBitmap = blur
                    }
                }
            }
        }
    )
}

@Composable
private fun SubcomposeAsyncImageScope.BlurShadow(blurBitmap: Bitmap?) = blurBitmap?.let {

    val crossfade = remember { CrossfadeTransition.Factory(1000) }

    Box(modifier = Modifier.requiredSize(0.dp)) {
        this@BlurShadow.SubcomposeAsyncImageContent(
            modifier = Modifier.requiredSize(it.width.pxToDp(), it.height.pxToDp()),
            painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(it)
                    .transitionFactory(crossfade)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .build()
            )
        )
    }
}
