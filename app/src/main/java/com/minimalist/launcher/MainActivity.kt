package com.minimalist.launcher

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.minimalist.launcher.ui.LauncherApp

class MainActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()

    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        vm.refreshRole()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            LauncherApp(
                vm,
                onLaunchSlot = ::launchSlotFor,
                onLaunchApp = { app ->
                    if (!launchComponent(this, app.key)) vm.openApps()
                },
                onRequestHome = ::requestHomeRole,
                onOpenHomeSettings = ::openHomeSettings,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
    }

    override fun onStop() {
        super.onStop()
        vm.onStopped()
    }

    /** Home button while we are already running: always land on the home screen. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN) vm.goHome()
    }

    private fun launchSlotFor(slot: Slot) {
        if (slot.hasBuiltIn && vm.prefs.assigned[slot] == null) { vm.openBuiltIn(slot); return }
        when (launchSlot(this, slot, vm.prefs.assigned[slot])) {
            LaunchResult.OK -> vm.drawerOpen = false
            LaunchResult.NEEDS_SETUP -> vm.pick(slot, Screen.Home)
            LaunchResult.MISSING -> vm.missingSlot = slot
        }
    }

    /** The user approves the change in the system's own dialog. Falls back to system settings. */
    private fun requestHomeRole() {
        val rm = getSystemService(RoleManager::class.java)
        if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
            try {
                roleRequest.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME))
                return
            } catch (_: ActivityNotFoundException) {
            }
        }
        openHomeSettings()
    }

    private fun openHomeSettings() {
        val attempts = listOf(
            Intent(Settings.ACTION_HOME_SETTINGS),
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )
        for (i in attempts) {
            try {
                startActivity(i)
                return
            } catch (_: ActivityNotFoundException) {
            }
        }
    }
}
