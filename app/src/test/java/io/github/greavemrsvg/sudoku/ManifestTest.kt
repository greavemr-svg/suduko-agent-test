package io.github.greavemrsvg.sudoku

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@Suppress("ktlint:standard:function-naming")
@RunWith(AndroidJUnit4::class)
class ManifestTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val packageManager = context.packageManager

    private fun packageInfo(flags: Int): PackageInfo = packageManager.getPackageInfo(context.packageName, flags)

    @Test
    fun AC22_merged_manifest_requests_no_network_permission() {
        val requested = packageInfo(PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty().toList()

        assertFalse(requested.contains(Manifest.permission.INTERNET))
        assertFalse(requested.contains(Manifest.permission.ACCESS_NETWORK_STATE))
        // Only the app's own signature-level permission added by AndroidX may appear.
        val allowed = "${context.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
        val unexpected = requested.filter { it != allowed }
        assertTrue("Unexpected permissions requested: $unexpected", unexpected.isEmpty())
    }

    @Test
    fun AC22_only_launcher_activity_is_exported() {
        val info =
            packageInfo(
                PackageManager.GET_ACTIVITIES or
                    PackageManager.GET_RECEIVERS or
                    PackageManager.GET_SERVICES or
                    PackageManager.GET_PROVIDERS,
            )
        val components =
            info.activities.orEmpty().map { it.name to it.exported } +
                info.receivers.orEmpty().map { it.name to it.exported } +
                info.services.orEmpty().map { it.name to it.exported } +
                info.providers.orEmpty().map { it.name to it.exported }

        val exported = components.filter { it.second }.map { it.first }
        assertEquals(listOf(MainActivity::class.java.name), exported)

        val launchers =
            packageManager.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName),
                0,
            )
        assertEquals(listOf(MainActivity::class.java.name), launchers.map { it.activityInfo.name })
        assertFalse(components.any { it.first == "androidx.profileinstaller.ProfileInstallReceiver" })
    }

    @Test
    fun AC19_main_activity_is_locked_to_portrait() {
        val activityInfo = packageManager.getActivityInfo(ComponentName(context, MainActivity::class.java), 0)

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activityInfo.screenOrientation)
    }
}
