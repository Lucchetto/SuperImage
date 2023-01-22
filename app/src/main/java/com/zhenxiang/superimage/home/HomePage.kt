package com.zhenxiang.superimage.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.transition.CrossfadeTransition
import com.zhenxiang.realesrgan.UpscalingModel
import com.zhenxiang.superimage.R
import com.zhenxiang.superimage.coil.BlurShadowTransformation
import com.zhenxiang.superimage.model.DataState
import com.zhenxiang.superimage.model.InputImage
import com.zhenxiang.superimage.model.OutputFormat
import com.zhenxiang.superimage.ui.form.MonoDropDownMenu
import com.zhenxiang.superimage.ui.mono.*
import com.zhenxiang.superimage.ui.theme.MonoTheme
import com.zhenxiang.superimage.ui.theme.border
import com.zhenxiang.superimage.ui.theme.spacing
import com.zhenxiang.superimage.ui.toDp
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(viewModel: HomePageViewModel) = Scaffold(
    topBar = { TopBar() }
) { padding ->

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        it?.let { viewModel.loadImage(it) }
    }
    val requestPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        LaunchedEffect(Unit) {
            if (
                ContextCompat.checkSelfPermission(
                    viewModel.getApplication(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Column(modifier = Modifier.padding(padding)) {

        val selectedImageState by viewModel.selectedImageFlow.collectAsStateWithLifecycle()

        ImagePreview(
            modifier = Modifier
                .weight(1f, true)
                .fillMaxWidth(),
            selectedImageState = selectedImageState,
            blurShadowTransformation = viewModel.blurShadowTransformation,
        ) { imagePicker.launch(HomePageViewModel.IMAGE_MIME_TYPE) }

        Options(
            upscalingModelFlow = viewModel.selectedUpscalingModelFlow,
            outputFormatFlow = viewModel.selectedOutputFormatFlow,
            selectedImageState = selectedImageState
        ) {
            viewModel.upscale()
        }
    }
}

@Composable
private fun TopBar() {
    MonoAppBar(
        title = { Text(stringResource(id = R.string.app_name)) }
    )
}

@Composable
private fun ImagePreview(
    modifier: Modifier,
    selectedImageState: DataState<InputImage, Unit>?,
    blurShadowTransformation: BlurShadowTransformation,
    onSelectedImage: () -> Unit
) {

    val crossfadeTransition = remember {
        CrossfadeTransition.Factory(125)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (selectedImageState) {
            is DataState.Success -> selectedImageState.data.let {
                Box(
                    modifier = Modifier.weight(1f, fill = false),
                    contentAlignment = Alignment.Center
                ) {
                    val blurShadow by blurShadowTransformation.blurBitmapFlow.collectAsStateWithLifecycle()
                    blurShadow?.let {
                        Box(
                            modifier = Modifier.requiredSize(0.dp)
                        ) {
                            AsyncImage(
                                modifier = Modifier
                                    .requiredSize(it.width.toDp(), it.height.toDp()),
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(it)
                                    .transitionFactory(crossfadeTransition)
                                    .memoryCachePolicy(CachePolicy.DISABLED)
                                    .build(),
                                alpha = 0.95f,
                                contentDescription = null
                            )
                        }
                    }

                    AsyncImage(
                        modifier = Modifier
                            .padding(
                                horizontal = MaterialTheme.spacing.level3,
                                vertical = MaterialTheme.spacing.level5,
                            )
                            .clip(MaterialTheme.shapes.large),
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(it.fileUri)
                            .memoryCachePolicy(CachePolicy.DISABLED)
                            .transitionFactory(crossfadeTransition)
                            .transformations(blurShadowTransformation)
                            .build(),
                        contentDescription = it.fileName
                    )
                }

                MonoButton(
                    modifier = Modifier.padding(
                        start = MaterialTheme.spacing.level5,
                        end = MaterialTheme.spacing.level5,
                        bottom = MaterialTheme.spacing.level5,
                    ),
                    onClick = onSelectedImage
                ) {
                    MonoButtonIcon(
                        painterResource(id = R.drawable.ic_image_24),
                        contentDescription = null
                    )
                    Text(stringResource(id = R.string.change_image_label))
                }
            }
            else -> {
                MonoButton(
                    onClick = onSelectedImage,
                ) {
                    Text(stringResource(id = R.string.select_image_label))
                }
            }
        }
    }
}

@Composable
private fun OutputFormatSelection(
    modifier: Modifier = Modifier,
    flow: MutableStateFlow<OutputFormat>
) {

    val selected by flow.collectAsStateWithLifecycle()

    MonoDropDownMenu(
        modifier = modifier,
        value = selected,
        label = { Text(stringResource(id = R.string.output_format_title)) },
        options = OutputFormat.VALUES,
        toStringAdapter = { it.formatName },
    ) {
        flow.tryEmit(it)
    }
}

@Composable
private fun ModelSelection(
    modifier: Modifier = Modifier,
    flow: MutableStateFlow<UpscalingModel>
) {

    val selected by flow.collectAsStateWithLifecycle()

    MonoDropDownMenu(
        modifier = modifier,
        value = selected,
        label = { Text(stringResource(id = R.string.selected_mode_label)) },
        options = UpscalingModel.VALUES,
        toStringAdapter = { stringResource(id = it.labelRes) },
    ) {
        flow.tryEmit(it)
    }
}

@Composable
private fun Options(
    upscalingModelFlow: MutableStateFlow<UpscalingModel>,
    outputFormatFlow: MutableStateFlow<OutputFormat>,
    selectedImageState: DataState<InputImage, Unit>?,
    onUpscaleClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .drawTopBorder(MaterialTheme.border.regular)
            .padding(
                horizontal = MaterialTheme.spacing.level3,
                vertical = MaterialTheme.spacing.level4
            )
    ) {
        Text(
            modifier = Modifier.padding(
                start = MaterialTheme.spacing.level3,
                end = MaterialTheme.spacing.level3,
                bottom = MaterialTheme.spacing.level4
            ),
            text = stringResource(id = R.string.upscaling_options_title),
            style = MaterialTheme.typography.headlineSmall
        )

        Row {
            ModelSelection(
                modifier = Modifier.weight(1f).padding(end = MaterialTheme.spacing.level4),
                flow = upscalingModelFlow
            )
            OutputFormatSelection(
                modifier = Modifier.weight(1f),
                flow = outputFormatFlow
            )
        }

        MonoButton(
            modifier = Modifier.padding(vertical = MaterialTheme.spacing.level5),
            enabled = selectedImageState is DataState.Success,
            onClick = onUpscaleClick,
        ) {
            MonoButtonIcon(
                painterResource(id = R.drawable.outline_auto_awesome_24),
                contentDescription = null
            )
            Text(stringResource(id = R.string.upscale_label))
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun OptionsPreview() = MonoTheme {
    Scaffold {
        Options(
            upscalingModelFlow = MutableStateFlow(UpscalingModel.X4_PLUS),
            outputFormatFlow = MutableStateFlow(OutputFormat.PNG),
            selectedImageState = DataState.Success(InputImage("", "".toUri()))
        ) {}
    }
}
