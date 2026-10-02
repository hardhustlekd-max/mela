package com.mela.ussdrunner.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mela.ussdrunner.MelaApplication
import com.mela.ussdrunner.MelaRoot

class WidgetRunActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val presetId = intent.getStringExtra(WidgetKeys.presetIdParam.name)
            ?: intent.getStringExtra("preset_id")
        val app = application as MelaApplication
        setContent {
            MelaRoot(app.container, presetId)
        }
    }
}
