package com.vivereply.roomready

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
        R.string.checklist_step_bed_linen,
        R.string.checklist_step_towels_toiletries,
        R.string.checklist_step_vacuum_mop,
        R.string.checklist_step_bathroom_deep_clean,
        R.string.checklist_step_minibar_restock,
        R.string.checklist_step_ac_lights_sockets,
        R.string.checklist_step_balcony_tidy,
        R.string.checklist_step_final_walkthrough,
        R.string.ticket_category_plumbing,
        R.string.ticket_category_electrical,
        R.string.ticket_category_linen,
        R.string.ticket_category_minibar,
        R.string.ticket_category_furniture,
        R.string.ticket_category_other,
        R.string.role_housekeeper,
        R.string.role_supervisor,
        R.string.setup_title,
        R.string.setup_subtitle,
        R.string.setup_address_label,
        R.string.setup_address_hint,
        R.string.setup_key_label,
        R.string.setup_key_hint,
        R.string.setup_name_label,
        R.string.setup_name_hint,
        R.string.setup_connect,
        R.string.setup_scan_qr,
        R.string.setup_paste_code,
        R.string.setup_paste_title,
        R.string.setup_paste_hint,
        R.string.setup_paste_apply,
        R.string.setup_working,
        R.string.setup_error_bad_address,
        R.string.setup_error_wrong_key,
        R.string.setup_error_server_down,
        R.string.setup_error_contract,
        R.string.setup_error_blank_name,
        R.string.setup_error_bad_key_format,
        R.string.qr_title,
        R.string.qr_hint,
        R.string.qr_no_camera,
        R.string.qr_need_permission,
        R.string.qr_grant,
        R.string.qr_error_not_roomready,
        R.string.qr_error_version,
        R.string.qr_error_missing_field,
        R.string.qr_error_bad_url,
        R.string.qr_error_bad_key,
        R.string.sync_last_synced,
        R.string.sync_last_synced_label,
        R.string.sync_never,
        R.string.sync_now,
        R.string.sync_working,
        R.string.home_today,
        R.string.home_no_rooms_title,
        R.string.home_no_rooms_copy,
        R.string.rooms_row_cd,
        R.string.occ_vacant,
        R.string.occ_occupied,
        R.string.occ_out_of_order,
        R.string.occ_unknown,
        R.string.tickets_open_title,
        R.string.tickets_none_after_sync,
        R.string.tickets_row_cd,
        R.string.activity_recent,
        R.string.activity_parked_flag,
        R.string.activity_empty_after_sync,
        R.string.activity_t_status_changed,
        R.string.activity_t_checklist_completed,
        R.string.activity_t_ticket_raised,
        R.string.activity_t_ticket_done,
        R.string.activity_t_sent_back,
        R.string.activity_t_bootstrap_imported,
        R.string.activity_t_sync_completed,
        R.string.activity_t_push_parked,
        R.string.profile_connected,
        R.string.profile_property,
        R.string.profile_staff,
        R.string.profile_key,
        R.string.profile_edit,
        R.string.action_close,
        R.string.action_retry,
      )

    allStringResIds.forEach { resId ->
      val enValue = enContext.getString(resId)
      val bnValue = bnContext.getString(resId)
      assertTrue("Missing EN string for $resId", enValue.isNotBlank())
      assertTrue("Missing BN string for $resId", bnValue.isNotBlank())
    }

    assertTrue(
      enContext.resources.getQuantityString(R.plurals.sync_pending, 1, 1).contains("1 waiting"),
    )
    assertTrue(
      bnContext.resources.getQuantityString(R.plurals.sync_pending, 5, 5)
        .contains("সিঙ্কের অপেক্ষায়"),
    )

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
