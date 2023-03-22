package com.zhenxiang.superimage

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.zhenxiang.superimage.intent.InputImageIntentManager
import org.koin.android.ext.android.inject

/**
 * Empty activity used to receive input image intents and route to [MainActivity]
 */
class IntentActivity : Activity() {

    private val inputImageIntentManager by inject<InputImageIntentManager>()

    override fun onCreate(savedInstanceState: Bundle?) {
        intent?.let { inputImageIntentManager.notifyNewIntent(it) }
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        super.onCreate(savedInstanceState)
    }
}