package app.focusflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.focusflow.timer.TimerController
import app.focusflow.ui.FocusApp
import app.focusflow.ui.FocusTheme
import app.focusflow.ui.MainViewModel

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val ui by vm.ui.collectAsStateWithLifecycle()
            FocusTheme(themeMode = ui.settings.theme, dynamicColor = ui.settings.dynamicColor) {
                FocusApp(vm = vm, ui = ui)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        TimerController.onAppReturned(this)
        TimerController.ensureAlarm(this)
        vm.refreshStats()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            TimerController.onAppLeft(this)
        }
    }
}
