package com.centralia.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.centralia.app.app.CentraliaRoot
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaTheme

/**
 * The single activity that hosts the whole Compose tree, standing in for the
 * SwiftUI `WindowGroup`.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as CentraliaApplication).container

        setContent {
            CentraliaTheme {
                // `.background(Color.centraliaCanvas.ignoresSafeArea())` at the root.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CentraliaColors.Canvas
                ) {
                    CentraliaRoot(container = container)
                }
            }
        }
    }
}
