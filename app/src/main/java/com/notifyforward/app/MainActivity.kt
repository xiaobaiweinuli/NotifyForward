package com.notifyforward.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.notifyforward.app.model.AppConfig
import com.notifyforward.app.model.ThemeMode
import com.notifyforward.app.ui.MainNavGraph
import com.notifyforward.app.ui.theme.NotifyForwardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = NotifyForwardApp.from(this)

        setContent {
            val config by app.configStore.configFlow
                .collectAsStateWithLifecycle(initialValue = AppConfig())

            val themeMode = runCatching { ThemeMode.valueOf(config.themeMode) }
                .getOrDefault(ThemeMode.SYSTEM)

            NotifyForwardTheme(themeMode = themeMode) {
                MainNavGraph()
            }
        }
    }
}
