package com.zhenxiang.superimage.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.request.ImageRequest
import coil.transition.CrossfadeTransition
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.essenty.lifecycle.ext.collectAsStateWithLifecycle
import com.zhenxiang.realesrgan.UpscalingModel
import com.zhenxiang.superimage.BuildConfig
import com.zhenxiang.superimage.R
import com.zhenxiang.superimage.intent.InputImageIntentManager
import com.zhenxiang.superimage.shared.model.Changelog
import com.zhenxiang.superimage.shared.model.DataState
import com.zhenxiang.superimage.shared.model.InputImage
import com.zhenxiang.superimage.shared.model.OutputFormat
import com.zhenxiang.superimage.navigation.RootComponent
import com.zhenxiang.superimage.ui.form.MonoDropDownMenu
import com.zhenxiang.superimage.ui.mono.*
import com.zhenxiang.superimage.ui.theme.*
import com.zhenxiang.superimage.ui.utils.RowSpacer
import com.zhenxiang.superimage.ui.utils.isLandscape
import com.zhenxiang.superimage.work.RealESRGANWorker
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(component: HomePageComponent) = component.viewModel.let { viewModel ->
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        it?.let { viewModel.loadImage(it) }
    }
    val requestPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
    }
    val openOutputImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.consumeWorkCompleted()
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val activity = LocalContext.current as Activity

    BoxWithConstraints {
        if (constraints.isLandscape) {
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing
            ) { padding ->
                val topPadding = padding.calculateTopPadding()
                val bottomPadding = padding.calculateBottomPadding()
                val layoutDirection = LocalLayoutDirection.current

                Row {
                    val selectedImageState by viewModel.selectedImageFlow.collectAsStateWithLifecycle()
                    val baseModifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(bottom = bottomPadding)

                    ImagePreview(
                        modifier = baseModifier.padding(
                            top = topPadding,
                            start = padding.calculateStartPadding(layoutDirection)
                        ),
                        selectedImageState = selectedImageState,
                        selectedModelState = viewModel.selectedUpscalingModelFlow.collectAsStateWithLifecycle(),
                    ) { imagePicker.launch(InputImageIntentManager.IMAGE_MIME_TYPE) }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(BorderThickness.regular)
                            .background(MaterialTheme.colorScheme.outline)
                    )

                    Column(
                        modifier = baseModifier.padding(end = padding.calculateEndPadding(layoutDirection))
                    ) {
                        TopBar(
                            component.navigation,
                            MonoAppBarDefaults.windowInsets.only(WindowInsetsSides.Top)
                        )
                        Options(
                            modifier = Modifier.fillMaxHeight(),
                            upscalingModelFlow = viewModel.selectedUpscalingModelFlow,
                            outputFormatFlow = viewModel.selectedOutputFormatFlow,
                            selectedImageState = selectedImageState,
                            onSelectImageClick = { imagePicker.launch(InputImageIntentManager.IMAGE_MIME_TYPE) },
                            onUpscaleClick = {
                                viewModel.upscale()
                                viewModel.showReviewFlowConditionally(activity)
                            }
                        )
                    }
                }
            }
        } else {
            Scaffold(
                topBar = { TopBar(component.navigation) },
                contentWindowInsets = WindowInsets.safeDrawing
            ) { padding ->
                Column(modifier = Modifier.padding(padding)) {

                    val selectedImageState by viewModel.selectedImageFlow.collectAsStateWithLifecycle()

                    ImagePreview(
                        modifier = Modifier
                            .weight(1f, true)
                            .fillMaxWidth(),
                        selectedImageState = selectedImageState,
                        selectedModelState = viewModel.selectedUpscalingModelFlow.collectAsStateWithLifecycle(),
                    ) { imagePicker.launch(InputImageIntentManager.IMAGE_MIME_TYPE) }

                    Options(
                        modifier = Modifier.drawTopBorder(MaterialTheme.border.regular),
                        upscalingModelFlow = viewModel.selectedUpscalingModelFlow,
                        outputFormatFlow = viewModel.selectedOutputFormatFlow,
                        selectedImageState = selectedImageState,
                        onSelectImageClick = { imagePicker.launch(InputImageIntentManager.IMAGE_MIME_TYPE) },
                        onUpscaleClick = {
                            viewModel.upscale()
                            viewModel.showReviewFlowConditionally(activity)
                        }
                    )
                }
            }
        }
    }

    val workProgressState by viewModel.workProgressFlow.collectAsStateWithLifecycle()

    workProgressState?.let {
        UpscalingWork(
            inputData = it.first,
            progress = it.second,
            onDismissRequest = {
                viewModel.consumeWorkCompleted()
                if (it.second is RealESRGANWorker.Progress.Success) {
                    viewModel.clearSelectedImage()
                }
            },
            onCancelClicked = { viewModel.cancelWork() },
            onRetryClicked = { viewModel.upscale() },
            onOpenOutputImageClicked = { intent -> openOutputImageLauncher.launch(intent) }
        )
    }

    val showChangelogState by viewModel.showChangelogFlow.collectAsStateWithLifecycle()
    ChangelogDialog(showChangelogState) { viewModel.showChangelogFlow.tryEmit(Changelog.Hide) }
}

@Composable
private fun ChangelogDialog(
    changelog: Changelog,
    onDismissRequest: () -> Unit
) {
    if (changelog != Changelog.Hide) {
        MonoAlertDialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            ),
            title = { Text(stringResource(id = R.string.changelog_dialog_title)) },
            content = { padding, _ ->
                LazyColumn(modifier = Modifier.padding(padding)) {
                    item { Text(
                        stringResource(id = R.string.version_template, BuildConfig.VERSION_NAME),
                        modifier = Modifier.padding(bottom = MaterialTheme.spacing.level3)
                    ) }
                    if (changelog is Changelog.Show) {
                        items(changelog.items) {
                            Text(stringResource(id = R.string.changelog_item_template, it))
                        }
                    }
                }
            },
            buttons = {
                RowSpacer()
                MonoCloseDialogButton(onDismissRequest)
            }
        )
    }
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun ChangelogDialogPreview() = MonoTheme {
    ChangelogDialog(
        Changelog.Show(listOf("IDK", "Stuff happened"))
    ) { }
}

@Composable
private fun TopBar(
    navigation: StackNavigation<RootComponent.Config>,
    windowInsets: WindowInsets = MonoAppBarDefaults.windowInsets,
) = MonoAppBar(
    title = { Text(stringResource(id = R.string.app_name)) },
    windowInsets = windowInsets
) {
    IconButton(
        onClick = { navigation.push(RootComponent.Config.Settings) }
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_gear_24),
            contentDescription = stringResource(id = R.string.settings)
        )
    }
}

@Composable
private fun ImagePreview(
    modifier: Modifier = Modifier,
    selectedImageState: DataState<InputImage, Unit>?,
    selectedModelState: State<UpscalingModel>,
    onSelectImageClick: () -> Unit
) {

    val crossfadeTransition = remember { CrossfadeTransition.Factory(125) }

    Column(
        modifier = modifier.padding(vertical = MaterialTheme.spacing.level5),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (selectedImageState) {
            is DataState.Success -> selectedImageState.data.let {
                BlurShadowImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(it.tempFile)
                        .transitionFactory(crossfadeTransition)
                        .build(),
                    contentDescription = it.fileName,
                    modifier = Modifier.weight(1f, fill = false),
                    imageModifier = Modifier
                        .padding(
                            start = MaterialTheme.spacing.level3,
                            end = MaterialTheme.spacing.level3,
                            bottom = MaterialTheme.spacing.level5,
                        )
                        .clip(MaterialTheme.shapes.large)
                )

                Text(
                    text = stringResource(id = R.string.original_image_resolution_label, it.width, it.height)
                )

                val selectedModel by selectedModelState
                Text(
                    text = stringResource(
                        id = R.string.output_image_resolution_label,
                        it.width * selectedModel.scale,
                        it.height * selectedModel.scale
                    )
                )

                if (it.mayRunOutOfMemory(selectedModel)) {
                    OutOfMemoryWarning()
                }
            }
            else -> StartWizard(onSelectImageClick)
        }
    }
}

@Composable
private fun OutOfMemoryWarning() = Row(
    verticalAlignment = Alignment.CenterVertically
) {
    Icon(
        modifier = Modifier
            .padding(end = MaterialTheme.spacing.level3)
            .size(MaterialTheme.typography.bodyLarge.fontSize.value.dp),
        painter = painterResource(R.drawable.ic_exclamation_octagon_24),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
    )
    Text(
        text = stringResource(R.string.out_of_memory_warning_hint),
        color = MaterialTheme.colorScheme.error
    )
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun OutOfMemoryWarningPreview() = MonoTheme {
    Surface(
        modifier = Modifier.padding(MaterialTheme.spacing.level4)
    ) {
        OutOfMemoryWarning()
    }
}

@Composable
private fun ColumnScope.StartWizard(onSelectImageClick: () -> Unit) {
    Text(
        modifier = Modifier.padding(
            bottom = MaterialTheme.spacing.level5
        ),
        text = stringResource(id = R.string.select_image_wizard_hint),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelLarge,
    )
    MonoButton(onClick = onSelectImageClick) {
        MonoButtonIcon(
            painterResource(id = R.drawable.ic_image_24),
            contentDescription = null
        )
        EllipsisText(
            stringResource(id = R.string.select_image_label)
        )
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun SetupWizardPreview() = MonoTheme {
    Scaffold {
        Column(
            modifier = Modifier
                .padding(MaterialTheme.spacing.level5)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StartWizard { }
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
    modifier: Modifier = Modifier,
    upscalingModelFlow: MutableStateFlow<UpscalingModel>,
    outputFormatFlow: MutableStateFlow<OutputFormat>,
    selectedImageState: DataState<InputImage, Unit>?,
    onSelectImageClick: () -> Unit,
    onUpscaleClick: () -> Unit
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(
                horizontal = MaterialTheme.spacing.level3,
                vertical = MaterialTheme.spacing.level4
            ),
        verticalArrangement = Arrangement.Bottom
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

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModelSelection(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = MaterialTheme.spacing.level4),
                flow = upscalingModelFlow
            )
            OutputFormatSelection(
                modifier = Modifier.weight(1f),
                flow = outputFormatFlow
            )
        }

        val imageSelected = selectedImageState is DataState.Success

        Row(
            modifier = Modifier
                .padding(vertical = MaterialTheme.spacing.level5)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (imageSelected) {
                MonoButton(
                    modifier = Modifier.padding(end = MaterialTheme.spacing.level4),
                    onClick = onSelectImageClick
                ) {
                    MonoButtonIcon(
                        painterResource(id = R.drawable.ic_image_24),
                        contentDescription = null
                    )
                    EllipsisText(
                        stringResource(id = R.string.change_image_label)
                    )
                }
            } else {
                RowSpacer()
            }

            UpscaleButton(enabled = imageSelected, onClick = onUpscaleClick)
        }
    }
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun OptionsPreview() = MonoTheme {
    Surface {
        Options(
            upscalingModelFlow = MutableStateFlow(UpscalingModel.X4_PLUS),
            outputFormatFlow = MutableStateFlow(OutputFormat.PNG),
            selectedImageState = DataState.Success(InputImage("", File(""), 0, 0)),
            onSelectImageClick = { },
            onUpscaleClick = { }
        )
    }
}
