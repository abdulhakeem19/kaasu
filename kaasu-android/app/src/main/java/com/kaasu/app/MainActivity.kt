package com.kaasu.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaasu.app.core.datastore.SettingsDataStore
import com.kaasu.app.feature.settings.AppLockScreen
import com.kaasu.app.feature.settings.WhatsNewDialog
import com.kaasu.app.navigation.AppNavHost
import com.kaasu.app.ui.theme.KaasuTheme
import dagger.hilt.android.AndroidEntryPoint

// FragmentActivity (not ComponentActivity) is required for androidx.biometric BiometricPrompt.
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val theme by appViewModel.appTheme.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val useDark = when (theme) {
                SettingsDataStore.Theme.DARK  -> true
                SettingsDataStore.Theme.LIGHT -> false
                else -> systemDark
            }
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.isAppearanceLightStatusBars = !useDark
            controller.isAppearanceLightNavigationBars = !useDark

            val lockEnabled by appViewModel.appLockEnabled.collectAsStateWithLifecycle()
            val locked by appViewModel.locked.collectAsStateWithLifecycle()
            val showWhatsNew by appViewModel.showWhatsNew.collectAsStateWithLifecycle()

            KaasuTheme(darkTheme = useDark) {
                AppNavHost(appViewModel = appViewModel)
                // Lock gate overlays everything while enabled and locked.
                val gated = lockEnabled && locked
                if (gated) {
                    AppLockScreen(onUnlock = { appViewModel.unlock() })
                }
                // What's-new shows once per version, only when unlocked.
                if (!gated && showWhatsNew) {
                    WhatsNewDialog(onClose = { appViewModel.dismissWhatsNew() })
                }
            }
        }
    }

    // Re-lock whenever the app leaves the foreground.
    override fun onStop() {
        super.onStop()
        appViewModel.lock()
    }
}
