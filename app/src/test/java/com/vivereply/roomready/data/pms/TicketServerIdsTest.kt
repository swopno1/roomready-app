package com.vivereply.roomready.data.pms

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TicketServerIdsTest {

  private fun store(): TicketServerIds =
    PrefsTicketServerIds(ApplicationProvider.getApplicationContext<Context>())

  @Test
  fun `save and lookup round trip both directions`() {
    val store = store()
    assertNull(store.serverIdFor("local-1"))
    assertNull(store.localIdFor("srv-9"))

    store.save("local-1", "srv-9")
    assertEquals("srv-9", store.serverIdFor("local-1"))
    assertEquals("local-1", store.localIdFor("srv-9"))
  }

  @Test
  fun `entries do not collide`() {
    val store = store()
    store.save("local-1", "srv-9")
    store.save("local-2", "srv-10")
    assertEquals("srv-10", store.serverIdFor("local-2"))
    assertEquals("local-2", store.localIdFor("srv-10"))
    assertNull(store.localIdFor("srv-missing"))
  }
}
