package com.example

import com.example.data.model.DEFAULT_ADMIN_ACCOUNT_HOLDER
import com.example.data.model.DEFAULT_ADMIN_ACCOUNT_NUMBER
import com.example.data.model.DEFAULT_ADMIN_BANK_NAME
import com.example.data.model.RADIO_ACCESS_FEE_NGN
import com.example.data.model.RADIO_ACCESS_USD_CENTS
import com.example.data.model.RADIO_CATEGORIES
import com.example.data.model.formatListenerCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Local JVM unit tests for BINTAFACE domain constants and formatters.
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun globalRadioDefaults_matchAdminRequirements() {
    assertEquals(2000L, RADIO_ACCESS_FEE_NGN)
    assertEquals(150L, RADIO_ACCESS_USD_CENTS)
    assertEquals("Francis Gregory", DEFAULT_ADMIN_ACCOUNT_HOLDER)
    assertEquals("FairMoney", DEFAULT_ADMIN_BANK_NAME)
    assertEquals("9038006221", DEFAULT_ADMIN_ACCOUNT_NUMBER)
    assertTrue("Global" in RADIO_CATEGORIES)
  }

  @Test
  fun formatListenerCount_formatsMillionsAndBillions() {
    assertEquals("0", formatListenerCount(0L))
    assertEquals("842", formatListenerCount(842L))
    assertEquals("1.5K", formatListenerCount(1_500L))
    assertEquals("2.4M", formatListenerCount(2_400_000L))
    assertEquals("1.2B", formatListenerCount(1_200_000_000L))
  }

  @Test
  fun themePresets_provideLightDarkAndDistinctSwatches() {
    val presets = com.example.ui.theme.AppThemePreset.entries
    assertEquals(4, presets.size)
    presets.forEach { preset ->
      assertEquals(3, com.example.ui.theme.previewSwatchesForPreset(preset).size)
      val light = com.example.ui.theme.colorSchemeFor(preset, darkTheme = false)
      val dark = com.example.ui.theme.colorSchemeFor(preset, darkTheme = true)
      assertTrue(light.primary != dark.background)
      assertEquals(preset, com.example.ui.theme.AppThemePreset.fromId(preset.id))
    }
    assertEquals(com.example.ui.theme.AppDarkMode.SYSTEM, com.example.ui.theme.AppDarkMode.fromId("unknown"))
  }

  @Test
  fun normalizeAccessUrl_preservesPlayStoreAndCustomDomains() {
    assertEquals(
      com.example.data.model.DEFAULT_APP_DOWNLOAD_URL,
      com.example.data.model.normalizeBintafaceAccessUrl("   ")
    )
    assertEquals(
      com.example.data.model.LEGACY_PLAY_APP_DOWNLOAD_URL,
      com.example.data.model.normalizeBintafaceAccessUrl(com.example.data.model.LEGACY_PLAY_APP_DOWNLOAD_URL)
    )
    assertEquals(
      "https://bintaface.org",
      com.example.data.model.normalizeBintafaceAccessUrl("https://bintaface.org")
    )
    assertEquals(
      com.example.data.model.LEGACY_PLAY_APP_DOWNLOAD_URL,
      com.example.data.model.buildBintafaceUserAccessUrl(
        com.example.data.model.LEGACY_PLAY_APP_DOWNLOAD_URL,
        "francis"
      )
    )
    assertEquals(
      "https://drive.google.com/file/d/abc123/view?usp=sharing",
      com.example.data.model.buildBintafaceUserAccessUrl(
        "https://drive.google.com/file/d/abc123/view?usp=sharing",
        "francis"
      )
    )
    assertEquals(
      "https://bintaface.org/u/francis",
      com.example.data.model.buildBintafaceUserAccessUrl("https://bintaface.org", "francis")
    )
  }
}
