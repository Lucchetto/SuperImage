package com.zhenxiang.superimage.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.zhenxiang.superimage.HomePageViewModel
import com.zhenxiang.superimage.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(viewModel: HomePageViewModel) = Scaffold { padding ->

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        it?.let { viewModel.upscale(it) }
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
        Button(
            onClick = { imagePicker.launch(HomePageViewModel.IMAGE_MIME_TYPE) }
        ) {
            Text(stringResource(id = R.string.upscale_label))
        }
    }
}
