package com.zhenxiang.superimage.ui.mono

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.transition.CrossfadeTransition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val BLUR_RADIUS = 70.dp

@Composable
fun BlurShadowImage(
    model: ImageRequest,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val blurShadowProvider = remember { BlurShadowProvider(context) }
    var blurShadowBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val crossfade = remember { CrossfadeTransition.Factory(1000) }

    SubcomposeLayout(modifier) { constraints ->

        val blurRadius = BLUR_RADIUS.value * density
        val blurRadiusInt = blurRadius.roundToInt()

        val imagePlaceable = subcompose(0) {
            AsyncImage(
                modifier = imageModifier,
                model = model,
                contentDescription = contentDescription,
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
        }[0].measure(constraints)

        val blurPlaceable = blurShadowBitmap?.let {
            subcompose(1) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(it)
                        .transitionFactory(crossfade)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .build(),
                    contentDescription = null
                )
            }[0].measure(
                Constraints.fixed(
                    imagePlaceable.width + blurRadiusInt * 2,
                    imagePlaceable.height + blurRadiusInt * 2
                )
            )
        }

        layout(imagePlaceable.width, imagePlaceable.height) {
            blurPlaceable?.place(blurRadiusInt * -1, blurRadiusInt * -1)
            imagePlaceable.place(0, 0)
        }
    }
}
