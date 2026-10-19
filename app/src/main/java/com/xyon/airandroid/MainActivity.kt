package com.xyon.airandroid

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.xyon.airandroid.ui.AirAndroidApp
import com.xyon.airandroid.ui.AirAndroidViewModel
import com.xyon.airandroid.ui.theme.AirAndroidTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AirAndroidViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            AirAndroidTheme {
                AirAndroidApp(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResumed()
    }

    override fun onPause() {
        viewModel.onAppPaused(this)
        super.onPause()
    }
}
