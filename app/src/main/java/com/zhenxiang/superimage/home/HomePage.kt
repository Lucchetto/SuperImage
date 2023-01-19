package com.zhenxiang.superimage.home

import android.Manifest
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zhenxiang.realesrgan.UpscalingModel
import com.zhenxiang.superimage.R
import com.zhenxiang.superimage.model.DataState
import com.zhenxiang.superimage.model.InputImage
import com.zhenxiang.superimage.ui.form.DropDownMenu
import com.zhenxiang.superimage.ui.mono.MonoAppBar
import com.zhenxiang.superimage.ui.mono.MonoButton
import com.zhenxiang.superimage.ui.mono.MonoButtonDefaults
import com.zhenxiang.superimage.ui.mono.drawTopBorder
import com.zhenxiang.superimage.ui.theme.MonoTheme
import com.zhenxiang.superimage.ui.theme.border
import com.zhenxiang.superimage.ui.theme.spacing
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
            selectedImageState = selectedImageState
        ) { imagePicker.launch(HomePageViewModel.IMAGE_MIME_TYPE) }

        Options(
            flow = viewModel.selectedUpscalingModelFlow,
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
    onSelectedImage: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (selectedImageState) {
            is DataState.Success -> selectedImageState.data.let {
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(
                            horizontal = MaterialTheme.spacing.level3,
                            vertical = MaterialTheme.spacing.level5,
                        )
                    ,
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.large),
                        model = ImageRequest.Builder(LocalContext.current).data(it.fileUri).build(),
                        contentDescription = it.fileName
                    )
                }

                Row(
                    modifier = Modifier.padding(
                        horizontal = MaterialTheme.spacing.level5,
                        vertical = MaterialTheme.spacing.level5,
                    )
                ) {
                    MonoButton(
                        onClick = onSelectedImage
                    ) {
                        Icon(
                            painterResource(id = R.drawable.ic_image_24),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = MaterialTheme.spacing.level3)
                                .size(MonoButtonDefaults.IconSize)
                        )
                        Text(stringResource(id = R.string.change_image_label))
                    }
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
private fun ModelSelection(flow: MutableStateFlow<UpscalingModel>) {

    val selected by flow.collectAsStateWithLifecycle()

    DropDownMenu(
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
    flow: MutableStateFlow<UpscalingModel>,
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

        ModelSelection(flow = flow)

        MonoButton(
            modifier = Modifier.padding(top = MaterialTheme.spacing.level5),
            enabled = selectedImageState is DataState.Success,
            onClick = onUpscaleClick,
        ) {
            Text(stringResource(id = R.string.upscale_label))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun OptionsPreview() = MonoTheme {
    Scaffold {
        Options(
            flow = MutableStateFlow(UpscalingModel.X4_PLUS),
            selectedImageState = DataState.Success(InputImage("", "".toUri()))
        ) {}
    }
}
