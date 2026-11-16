package com.vivereply.roomready.data.pms

import com.vivereply.roomready.data.Clock
import com.vivereply.roomready.data.sync.OutgoingMutation
import com.vivereply.roomready.data.sync.SyncEntityType
import com.vivereply.roomready.data.sync.SyncOp
import com.vivereply.roomready.domain.model.StaffMember
import com.vivereply.roomready.domain.model.StaffRole
import com.vivereply.roomready.security.SecureCredentialStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PmsOutboundTransportTest {

  @Test
  fun `send signs with vault key and app actor`() =
    runTest {
      val api = FakePmsApi()
      val vault = FakeVault(key = "rk.k597.secret-77", staff = StaffMember("Ayesha", StaffRole.HOUSEKEEPER))
      val conn = FakeStore(baseUrl = PmsAddress("https://pms.demo13.com"))
      val transport = PmsOutboundTransport(api, vault, conn, FakeClock())

      transport.send(OutgoingMutation(7L, SyncEntityType.UNIT_STATUS, "u-101", SyncOp.UPSERT, "{\"a\":1}"))

      val pushed = api.pushed.single()
      assertEquals("u-101", pushed.entityId)
      assertEquals("rk.k597.secret-77", pushed.apiKey)
      assertEquals("app:k597:Ayesha", pushed.actor)
      assertEquals(5_000L, pushed.atEpochMillis)
    }

  @Test
  fun `missing url key or identity aborts with auth error`() =
    runTest {
      val full = FakeVault(key = "rk.k1.s", staff = StaffMember("A", StaffRole.HOUSEKEEPER))
      val cases =
        listOf(
          Triple(FakePmsApi(), full, FakeStore(baseUrl = null)),
          Triple(FakePmsApi(), FakeVault(key = null, staff = StaffMember("A", StaffRole.HOUSEKEEPER)), FakeStore(baseUrl = PmsAddress("https://a.com"))),
          Triple(FakePmsApi(), FakeVault(key = "not-a-key", staff = StaffMember("A", StaffRole.HOUSEKEEPER)), FakeStore(baseUrl = PmsAddress("https://a.com"))),
          Triple(FakePmsApi(), FakeVault(key = "rk.k1.s", staff = null), FakeStore(baseUrl = PmsAddress("https://a.com"))),
        )
      cases.forEach { (api, vault, conn) ->
        try {
          PmsOutboundTransport(api, vault, conn, FakeClock())
            .send(OutgoingMutation(1L, SyncEntityType.TICKET, "t-1", SyncOp.UPSERT, "{}"))
          throw AssertionError("expected PmsAuthException")
        } catch (e: PmsAuthException) {
          assertTrue(e.message!!.isNotBlank())
        }
        assertTrue(api.pushed.isEmpty())
      }
    }

  class FakePmsApi(var bootstrapBody: String = "{}", var bootstrapError: PmsApiException? = null) : PmsApi {
    val pushed = mutableListOf<PushRequest>()

    override suspend fun fetchBootstrap(baseUrl: PmsAddress, apiKey: String): String {
      bootstrapError?.let { throw it }
      return bootstrapBody
    }

    override suspend fun push(request: PushRequest) {
      pushed += request
    }
  }

  class FakeVault(private var key: String?, private var staff: StaffMember?) : SecureCredentialStore {
    override fun saveApiKey(apiKey: String) {
      key = apiKey
    }

    override fun hasApiKey(): Boolean = key != null

    override fun apiKeyForAuth(): String? = key

    override fun apiKeyLast4(): String = key?.takeLast(4) ?: ""

    override fun maskedApiKey(): String = key?.let { "••••" + it.takeLast(4) } ?: ""

    override fun saveStaffIdentity(staff: StaffMember) {
      this.staff = staff
    }

    override fun staffIdentity(): StaffMember? = staff

    override fun clearApiKey() {
      key = null
    }

    override fun clearAll() {
      key = null
      staff = null
    }
  }

  class FakeStore(private var baseUrl: PmsAddress?) : ConnectionStore {
    private var lastSynced: Long? = null
    private var property: String? = null

    override fun baseUrl(): PmsAddress? = baseUrl

    override fun saveBaseUrl(address: PmsAddress) {
      baseUrl = address
    }

    override fun lastSyncedAt(): Long? = lastSynced

    override fun saveLastSyncedAt(atEpochMillis: Long) {
      lastSynced = atEpochMillis
    }

    override fun propertyName(): String? = property

    override fun saveProperty(id: String?, name: String?) {
      property = name
    }

    override fun clear() {
      baseUrl = null
      lastSynced = null
      property = null
    }
  }

  private class FakeClock : Clock {
    override fun nowEpochMillis(): Long = 5_000L
  }
}
