package com.vivereply.roomready.data.pms

import com.vivereply.roomready.R
import com.vivereply.roomready.data.Clock
import com.vivereply.roomready.data.local.SyncMutationEntity
import com.vivereply.roomready.data.local.SyncQueueDao
import com.vivereply.roomready.data.sync.OutgoingMutation
import com.vivereply.roomready.data.sync.SyncEntityType
import com.vivereply.roomready.data.sync.SyncOp
import com.vivereply.roomready.domain.model.PmsHousekeeping
import com.vivereply.roomready.domain.model.StaffMember
import com.vivereply.roomready.domain.model.StaffRole
import com.vivereply.roomready.domain.model.TicketStatus
import com.vivereply.roomready.security.SecureCredentialStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Transport mapping over a fake API. Robolectric: stored payloads parse as JSON. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PmsOutboundTransportTest {

  private val url = PmsAddress("https://pms.demo13.com")
  private val key = "rk.k597.secret-77"
  private val staff = StaffMember("Ayesha", StaffRole.HOUSEKEEPER)

  private fun transport(
    api: FakePmsApi = FakePmsApi(),
    vault: FakeVault = FakeVault(key, staff),
    conn: FakeStore = FakeStore(url),
    queue: FakeQueue = FakeQueue(),
    ticketIds: FakeTicketIds = FakeTicketIds(),
  ): Pair<PmsOutboundTransport, FakePmsApi> =
    PmsOutboundTransport(api, vault, conn, queue, ticketIds, ::testStrings) to api

  private fun mutation(
    type: SyncEntityType,
    entityId: String,
    payload: String,
  ): OutgoingMutation = OutgoingMutation(7L, type, entityId, SyncOp.UPSERT, payload)

  private fun unitPayload(localState: String, extra: String = ""): String =
    "{\"unitId\":\"u-101\",\"housekeeping\":\"DIRTY\",\"localState\":\"$localState\"," +
      "\"actor\":\"Ayesha\",\"at\":1000$extra}"

  @Test
  fun `start cleaning makes zero api calls`() =
    runTest {
      val (transport, api) = transport()
      transport.send(mutation(SyncEntityType.UNIT_STATUS, "u-101", unitPayload("CLEANING")))
      assertTrue(api.statusCalls.isEmpty())
      assertTrue(api.creates.isEmpty())
      assertTrue(api.updates.isEmpty())
    }

  @Test
  fun `checklist entries resolve without wire calls`() =
    runTest {
      val (transport, api) = transport()
      transport.send(mutation(SyncEntityType.CHECKLIST, "u-101", "{\"unitId\":\"u-101\"}"))
      assertTrue(api.statusCalls.isEmpty())
    }

  @Test
  fun `finish posts mark clean with staff name and skip note`() =
    runTest {
      val (transport, api) = transport()
      transport.send(
        mutation(
          SyncEntityType.UNIT_STATUS,
          "u-101",
          unitPayload("CLEANED", ",\"skipReason\":\"too dark\""),
        ),
      )
      val call = api.statusCalls.single()
      assertEquals("u-101", call.unitId)
      assertEquals(PmsHousekeeping.DIRTY, call.body.expected)
      assertEquals(StatusAction.MARK_CLEAN, call.body.action)
      assertEquals("Ayesha", call.body.staffName)
      assertEquals("too dark", call.body.note)
      assertEquals("rk.k597.secret-77", call.apiKey)
    }

  @Test
  fun `send back posts inspect fail with the reason`() =
    runTest {
      val (transport, api) = transport()
      transport.send(
        mutation(SyncEntityType.UNIT_STATUS, "u-101", unitPayload("DIRTY", ",\"note\":\"dust\"")),
      )
      val call = api.statusCalls.single()
      assertEquals(StatusAction.INSPECT_FAIL, call.body.action)
      assertEquals(PmsHousekeeping.CLEAN, call.body.expected)
      assertEquals("dust", call.body.note)
    }

  @Test
  fun `403 carries the permission flag message`() =
    runTest {
      val api = FakePmsApi(callError = PmsApiException("POST room status failed", 403))
      val (transport, _) = transport(api = api)
      try {
        transport.send(mutation(SyncEntityType.UNIT_STATUS, "u-101", unitPayload("CLEANED")))
        throw AssertionError("expected PmsRefusedException")
      } catch (e: PmsRefusedException) {
        assertEquals(403, e.httpStatus)
        assertEquals("HTTP 403", e.lastError)
        assertTrue(e.flagDetail.contains("u-101"))
        assertTrue(e.flagDetail.contains("NO-PERM"))
      }
    }

  @Test
  fun `403 on ticket done under housekeeper key gets the supervisor message`() =
    runTest {
      val api = FakePmsApi(callError = PmsApiException("PATCH ticket failed", 403))
      val ticketIds = FakeTicketIds().apply { save("local-1", "srv-9") }
      val (transport, _) = transport(api = api, ticketIds = ticketIds)
      try {
        transport.send(mutation(SyncEntityType.TICKET_STATUS, "local-1", "{}"))
        throw AssertionError("expected PmsRefusedException")
      } catch (e: PmsRefusedException) {
        assertTrue(e.flagDetail.contains("NEEDS-SUP"))
      }
    }

  @Test
  fun `401 propagates untouched for the drain stop`() =
    runTest {
      val api = FakePmsApi(callError = PmsApiException("POST room status failed", 401))
      val (transport, _) = transport(api = api)
      try {
        transport.send(mutation(SyncEntityType.UNIT_STATUS, "u-101", unitPayload("CLEANED")))
        throw AssertionError("expected PmsApiException")
      } catch (e: PmsApiException) {
        assertEquals(401, e.statusCode)
      }
    }

  @Test
  fun `retryable failures propagate for backoff`() =
    runTest {
      val api = FakePmsApi(callError = PmsApiException("POST room status failed", 503))
      val (transport, _) = transport(api = api)
      try {
        transport.send(mutation(SyncEntityType.UNIT_STATUS, "u-101", unitPayload("CLEANED")))
        throw AssertionError("expected PmsApiException")
      } catch (e: PmsApiException) {
        assertEquals(503, e.statusCode)
      }
    }

  @Test
  fun `ticket create stores the returned server id`() =
    runTest {
      val api = FakePmsApi(nextCreateId = "srv-9")
      val ticketIds = FakeTicketIds()
      val (transport, _) = transport(api = api, ticketIds = ticketIds)
      transport.send(
        mutation(
          SyncEntityType.TICKET,
          "local-1",
          "{\"ticketId\":\"local-1\",\"unitId\":\"u-101\",\"category\":\"PLUMBING\",\"note\":\"leak\"}",
        ),
      )
      val created = api.creates.single()
      assertEquals("u-101", created.body.unitId)
      assertEquals("Plumbing", created.body.title)
      assertEquals("leak", created.body.description)
      assertEquals("NORMAL", created.body.priority)
      assertEquals("Ayesha", created.body.staffName)
      assertEquals("srv-9", ticketIds.serverIdFor("local-1"))
    }

  @Test
  fun `ticket done patches the server id`() =
    runTest {
      val api = FakePmsApi()
      val ticketIds = FakeTicketIds().apply { save("local-1", "srv-9") }
      val (transport, _) = transport(api = api, ticketIds = ticketIds)
      transport.send(mutation(SyncEntityType.TICKET_STATUS, "local-1", "{}"))
      val updated = api.updates.single()
      assertEquals("srv-9", updated.serverId)
      assertEquals(TicketStatus.DONE, updated.body.status)
      assertEquals("Ayesha", updated.body.staffName)
    }

  @Test
  fun `ticket done defers while its create is still queued`() =
    runTest {
      val api = FakePmsApi()
      val queue = FakeQueue(unsynced = 1)
      val (transport, _) = transport(api = api, queue = queue)
      try {
        transport.send(mutation(SyncEntityType.TICKET_STATUS, "local-1", "{}"))
        throw AssertionError("expected DeferEntryException")
      } catch (e: DeferEntryException) {
        assertTrue(api.updates.isEmpty())
      }
    }

  @Test
  fun `ticket done without server id or queued create parks as untranslatable`() =
    runTest {
      val (transport, _) = transport()
      try {
        transport.send(mutation(SyncEntityType.TICKET_STATUS, "local-1", "{}"))
        throw AssertionError("expected PmsRefusedException")
      } catch (e: PmsRefusedException) {
        assertEquals(null, e.httpStatus)
        assertEquals("UNTRANSLATABLE", e.lastError)
        assertTrue(e.flagDetail.contains("missing-server-id"))
      }
    }

  @Test
  fun `corrupt entries park as untranslatable`() =
    runTest {
      val (transport, _) = transport()
      try {
        transport.send(mutation(SyncEntityType.UNIT_STATUS, "u-101", "not-json{{"))
        throw AssertionError("expected PmsRefusedException")
      } catch (e: PmsRefusedException) {
        assertEquals("UNTRANSLATABLE", e.lastError)
      }
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
          PmsOutboundTransport(api, vault, conn, FakeQueue(), FakeTicketIds(), ::testStrings)
            .send(OutgoingMutation(1L, SyncEntityType.TICKET, "t-1", SyncOp.UPSERT, "{}"))
          throw AssertionError("expected PmsAuthException")
        } catch (e: PmsAuthException) {
          assertTrue(e.message!!.isNotBlank())
        }
        assertTrue(api.statusCalls.isEmpty())
        assertTrue(api.creates.isEmpty())
      }
    }

  data class RecordedStatus(val unitId: String, val body: StatusBody, val apiKey: String)
  data class RecordedCreate(val body: TicketCreateBody)
  data class RecordedUpdate(val serverId: String, val body: TicketUpdateBody)

  class FakePmsApi(
    var bootstrapBody: String = "{}",
    var bootstrapError: PmsApiException? = null,
    var callError: PmsApiException? = null,
    var nextCreateId: String = "srv-9",
    var ticketLists: Map<String, String> = emptyMap(),
    var ticketsError: PmsApiException? = null,
  ) : PmsApi {
    var bootstrapCalls = 0
    var statusAttempts = 0
    val statusCalls = mutableListOf<RecordedStatus>()
    val creates = mutableListOf<RecordedCreate>()
    val updates = mutableListOf<RecordedUpdate>()
    val fetchedLists = mutableListOf<String>()

    override suspend fun fetchBootstrap(baseUrl: PmsAddress, apiKey: String): String {
      bootstrapCalls++
      bootstrapError?.let { throw it }
      return bootstrapBody
    }

    override suspend fun postRoomStatus(
      baseUrl: PmsAddress,
      apiKey: String,
      unitId: String,
      body: StatusBody,
    ) {
      statusAttempts++
      callError?.let { throw it }
      statusCalls += RecordedStatus(unitId, body, apiKey)
    }

    override suspend fun createTicket(
      baseUrl: PmsAddress,
      apiKey: String,
      body: TicketCreateBody,
    ): String {
      callError?.let { throw it }
      creates += RecordedCreate(body)
      return nextCreateId
    }

    override suspend fun updateTicket(
      baseUrl: PmsAddress,
      apiKey: String,
      serverId: String,
      body: TicketUpdateBody,
    ) {
      callError?.let { throw it }
      updates += RecordedUpdate(serverId, body)
    }

    override suspend fun fetchTickets(
      baseUrl: PmsAddress,
      apiKey: String,
      status: TicketStatus,
    ): String {
      ticketsError?.let { throw it }
      fetchedLists += status.name
      return ticketLists[status.name] ?: "[]"
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

  class FakeTicketIds : TicketServerIds {
    private val forward = mutableMapOf<String, String>()

    override fun save(localId: String, serverId: String) {
      forward[localId] = serverId
    }

    override fun serverIdFor(localId: String): String? = forward[localId]

    override fun localIdFor(serverId: String): String? =
      forward.entries.firstOrNull { it.value == serverId }?.key
  }

  class FakeQueue(private val unsynced: Int = 0) : SyncQueueDao {
    override suspend fun enqueue(mutation: SyncMutationEntity): Long = TODO()

    override suspend fun peekDue(now: Long, limit: Int): List<SyncMutationEntity> = TODO()

    override suspend fun markInFlight(ids: List<Long>, now: Long) = TODO()

    override suspend fun markDelivered(id: Long) = TODO()

    override suspend fun reschedule(id: Long, attempts: Int, dueAt: Long, now: Long) = TODO()

    override suspend fun markFailed(id: Long, attempts: Int, now: Long) = TODO()

    override suspend fun park(id: Long, error: String, now: Long) = TODO()

    override suspend fun resetStaleInFlight(cutoff: Long, now: Long): Int = TODO()

    override suspend fun unsyncedCountFor(
      entityType: SyncEntityType,
      entityId: String,
    ): Int = unsynced

    override suspend fun pendingCount(): Int = TODO()
  }

  private class FakeClock : Clock {
    override fun nowEpochMillis(): Long = 5_000L
  }
}

fun testStrings(resId: Int): String =
  when (resId) {
    R.string.sync_parked_no_permission -> "NO-PERM"
    R.string.sync_parked_ticket_done_role -> "NEEDS-SUP"
    else -> "?"
  }
