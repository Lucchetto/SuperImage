package com.zhenxiang.tfrealesrgan.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(viewModel: HomeViewModel) {

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        viewModel.load(it)
    }

    Scaffold {
        Column(modifier = Modifier.padding(it)) {
            Button(
                onClick = {
                    imagePicker.launch("image/*")
                }
            ) {
                Text("Upscale")
            }
        }
    }
}
