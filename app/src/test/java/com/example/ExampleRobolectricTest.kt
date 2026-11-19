package com.example

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("BINTAFACE", appName)
  }

  @Test
  fun `merged manifest excludes adservices measurement permissions`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val pkgInfo = context.packageManager.getPackageInfo(
      context.packageName,
      PackageManager.GET_PERMISSIONS
    )
    val permissions = pkgInfo.requestedPermissions?.toSet().orEmpty()
    assertFalse(permissions.contains("android.permission.ACCESS_ADSERVICES_ATTRIBUTION"))
    assertFalse(permissions.contains("android.permission.ACCESS_ADSERVICES_AD_ID"))
    assertFalse(permissions.contains("android.permission.ACCESS_ADSERVICES_TOPICS"))
    assertTrue(permissions.contains("com.google.android.gms.permission.AD_ID"))
  }
}
