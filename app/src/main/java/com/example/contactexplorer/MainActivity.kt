/*
 * Contact Explorer Enterprise Android
 * A CTYV PRODUCT
 * Copyright © 2026 Priyanshu Sharma (CTYV) — A CTYV Product. All rights reserved.
 * Distributed under the MIT License (with Attribution).
 */

package com.example.contactexplorer

import android.os.Bundle
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.contactexplorer.ui.navigation.ContactExplorerNavHost
import com.example.contactexplorer.ui.theme.ContactExplorerTheme

class MainActivity : ComponentActivity() {
    private val vm: ExplorerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Fluid transition: animated icon scales up and fades as the app surface reveals
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val iconView = splashScreenViewProvider.iconView
            val splashView = splashScreenViewProvider.view

            iconView.animate()
                .scaleX(1.15f)
                .scaleY(1.15f)
                .alpha(0f)
                .setDuration(350L)
                .setInterpolator(AccelerateInterpolator())
                .start()

            splashView.animate()
                .alpha(0f)
                .setDuration(350L)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    splashScreenViewProvider.remove()
                }
                .start()
        }

        setContent {
            ContactExplorerTheme(themeMode = vm.themeMode) {
                ContactExplorerNavHost(vm = vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
        vm.message?.let {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            vm.clearMessage()
        }
    }
}
