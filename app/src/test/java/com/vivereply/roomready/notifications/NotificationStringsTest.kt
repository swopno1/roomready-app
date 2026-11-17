package com.vivereply.roomready.notifications

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.vivereply.roomready.R
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Real notification strings resolve in both languages and feed the payload logic. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NotificationStringsTest {

  private val baseContext: Context get() = ApplicationProvider.getApplicationContext()

  @Test
  fun `morning payload assembles from real English strings`() {
    val context = baseContext.createLocalizedContext("en")
    val payload =
      MorningSummary.build(
        dirtyCount = 3,
        waitingCount = 1,
        title = context.getString(R.string.notif_morning_title),
        dirtyLine = { n -> context.resources.getQuantityString(R.plurals.notif_morning_body, n, n) },
        waitingLine = { n -> context.resources.getQuantityString(R.plurals.notif_inspect_body, n, n) },
      )!!
    assertEquals("Today's rooms", payload.title)
    assertEquals("3 rooms need cleaning\n1 room ready to inspect", payload.body)
  }

  @Test
  fun `morning payload assembles from real Bengali strings`() {
    val context = baseContext.createLocalizedContext("bn")
    val payload =
      MorningSummary.build(
        dirtyCount = 3,
        waitingCount = 0,
        title = context.getString(R.string.notif_morning_title),
        dirtyLine = { n -> context.resources.getQuantityString(R.plurals.notif_morning_body, n, n) },
        waitingLine = { n -> context.resources.getQuantityString(R.plurals.notif_inspect_body, n, n) },
      )!!
    assertEquals("আজকের রুম", payload.title)
    assertTrue(payload.body.contains("পরিষ্কার দরকার"))
  }

  @Test
  fun `inspection nudge assembles from real strings in both languages`() {
    val en = baseContext.createLocalizedContext("en")
    val enPayload =
      WaitingInspection.build(
        waitingCount = 2,
        title = en.getString(R.string.notif_inspect_title),
        body = { n -> en.resources.getQuantityString(R.plurals.notif_inspect_body, n, n) },
      )!!
    assertEquals("Rooms waiting for inspection", enPayload.title)
    assertEquals("2 rooms ready to inspect", enPayload.body)

    val bn = baseContext.createLocalizedContext("bn")
    val bnPayload =
      WaitingInspection.build(
        waitingCount = 2,
        title = bn.getString(R.string.notif_inspect_title),
        body = { n -> bn.resources.getQuantityString(R.plurals.notif_inspect_body, n, n) },
      )!!
    assertEquals("পরিদর্শনের অপেক্ষায় রুম", bnPayload.title)
    assertTrue(bnPayload.body.contains("পরিদর্শনের জন্য প্রস্তুত"))
  }

  @Test
  fun `silent cases post nothing in either language`() {
    listOf("en", "bn").forEach { tag ->
      val context = baseContext.createLocalizedContext(tag)
      assertNull(
        MorningSummary.build(
          0,
          0,
          context.getString(R.string.notif_morning_title),
          { n -> context.resources.getQuantityString(R.plurals.notif_morning_body, n, n) },
          { n -> context.resources.getQuantityString(R.plurals.notif_inspect_body, n, n) },
        ),
      )
      assertNull(
        WaitingInspection.build(
          0,
          context.getString(R.string.notif_inspect_title),
          { n -> context.resources.getQuantityString(R.plurals.notif_inspect_body, n, n) },
        ),
      )
    }
  }

  private fun Context.createLocalizedContext(languageTag: String): Context {
    val config = Configuration(resources.configuration)
    config.setLocale(Locale.forLanguageTag(languageTag))
    return createConfigurationContext(config)
  }
}
