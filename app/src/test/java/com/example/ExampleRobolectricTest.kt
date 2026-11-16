package com.example

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `app name matches RoomReady`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("RoomReady", context.getString(R.string.app_name))
  }

  @Test
  fun `all English and Bengali string keys have full non-empty parity`() {
    val baseContext = ApplicationProvider.getApplicationContext<Context>()
    val enContext = baseContext.createLocalizedContext("en")
    val bnContext = baseContext.createLocalizedContext("bn")

    val allStringResIds =
      listOf(
        R.string.app_name,
        R.string.nav_home,
        R.string.nav_rooms,
        R.string.nav_tickets,
        R.string.nav_activity,
        R.string.nav_profile,
        R.string.status_offline_not_linked,
        R.string.brand_lockup_content_description,
        R.string.brand_tagline,
        R.string.status_dirty,
        R.string.status_cleaning,
        R.string.status_cleaned,
        R.string.status_ready,
        R.string.empty_home_title,
        R.string.empty_home_copy,
        R.string.empty_home_action,
        R.string.empty_home_artwork_cd,
        R.string.empty_rooms_title,
        R.string.empty_rooms_copy,
        R.string.empty_rooms_action,
        R.string.empty_rooms_artwork_cd,
        R.string.empty_tickets_title,
        R.string.empty_tickets_copy,
        R.string.empty_tickets_action,
        R.string.empty_tickets_artwork_cd,
        R.string.empty_activity_title,
        R.string.empty_activity_copy,
        R.string.empty_activity_action,
        R.string.empty_activity_artwork_cd,
        R.string.empty_profile_title,
        R.string.empty_profile_copy,
        R.string.empty_profile_action,
        R.string.empty_profile_artwork_cd,
        R.string.language_label,
        R.string.language_english,
        R.string.language_bengali,
        R.string.action_connect_prompt_feedback,
      )

    allStringResIds.forEach { resId ->
      val enValue = enContext.getString(resId)
      val bnValue = bnContext.getString(resId)
      assertTrue("Missing EN string for $resId", enValue.isNotBlank())
      assertTrue("Missing BN string for $resId", bnValue.isNotBlank())
    }

    assertNotEquals(
      enContext.getString(R.string.empty_home_title),
      bnContext.getString(R.string.empty_home_title),
    )
    assertNotEquals(
      enContext.getString(R.string.nav_rooms),
      bnContext.getString(R.string.nav_rooms),
    )
  }

  @Test
  fun `adaptive launcher and splash drawables resolve cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertNotNull(ContextCompat.getDrawable(context, R.drawable.ic_launcher_background))
    assertNotNull(ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground))
    assertNotNull(ContextCompat.getDrawable(context, R.drawable.ic_launcher_monochrome))
    assertNotNull(ContextCompat.getDrawable(context, R.drawable.ic_splash_mark))
    assertNotNull(ContextCompat.getDrawable(context, R.drawable.splash_background_compat))
    assertNotNull(ContextCompat.getDrawable(context, R.mipmap.ic_launcher))
    assertNotNull(ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round))
  }

  private fun Context.createLocalizedContext(languageTag: String): Context {
    val config = Configuration(resources.configuration)
    config.setLocale(Locale.forLanguageTag(languageTag))
    return createConfigurationContext(config)
  }
}
