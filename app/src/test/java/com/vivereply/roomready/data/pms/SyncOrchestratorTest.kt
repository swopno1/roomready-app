package com.vivereply.roomready.data.pms

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vivereply.roomready.R
import com.vivereply.roomready.data.Clock
import com.vivereply.roomready.data.local.RoomReadyDatabase
import com.vivereply.roomready.data.local.SyncMutationEntity
import com.vivereply.roomready.data.pms.PmsOutboundTransportTest.FakePmsApi
import com.vivereply.roomready.data.pms.PmsOutboundTransportTest.FakeStore
import com.vivereply.roomready.data.pms.PmsOutboundTransportTest.FakeTicketIds
import com.vivereply.roomready.data.pms.PmsOutboundTransportTest.FakeVault
import com.vivereply.roomready.data.repository.RoomRepository
import com.vivereply.roomready.data.repository.TicketRepository
import com.vivereply.roomready.data.sync.SyncEntityType
import com.vivereply.roomready.data.sync.SyncOp
import com.vivereply.roomready.data.sync.SyncPayloads
import com.vivereply.roomready.domain.model.ActivityType
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.PmsHousekeeping
import com.vivereply.roomready.domain.model.StaffMember
import com.vivereply.roomready.domain.model.StaffRole
import com.vivereply.roomready.domain.model.TicketCategory
import com.vivereply.roomready.domain.model.TicketStatus
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SyncOrchestratorTest {

  private lateinit var db: RoomReadyDatabase
  private lateinit var rooms: RoomRepository
  private lateinit var tickets: TicketRepository
  private lateinit var api: FakePmsApi
  private lateinit var vault: FakeVault
  private lateinit var conn: FakeStore
  private lateinit var ticketIds: FakeTicketIds
  private lateinit var orchestrator: SyncOrchestrator

  @Before
  fun setUp() {
    db =
      Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        RoomReadyDatabase::class.java,
      ).allowMainThreadQueries().build()
    rooms =
      RoomRepository(
        db.roomUnitDao(),
        db.roomTypeDao(),
        db.staffRosterDao(),
        db.checklistDao(),
        db.cleaningEvidenceDao(),
        db.activityDao(),
        db.syncQueueDao(),
        FakeClock(),
      )
    api = FakePmsApi()
    vault = FakeVault(null, null)
    conn = FakeStore(null)
    ticketIds = FakeTicketIds()
    tickets =
      TicketRepository(
        db.roomUnitDao(),
        db.ticketDao(),
        db.activityDao(),
        db.syncQueueDao(),
        FakeClock(),
      )
    orchestrator =
      SyncOrchestrator(
        api,
        vault,
        conn,
        rooms,
        tickets,
        db.syncQueueDao(),
        db.activityDao(),
        ticketIds,
        FakeClock(),
        ::testStrings,
      )
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `connect validates stores and imports the board`() =
    runTest {
      api.bootstrapBody = BOOTSTRAP
      val report = orchestrator.validateAndConnect("https://pms.demo13.com", "rk.k597.s3cret", "Ayesha").getOrThrow()

      assertEquals(1, report.roomTypes)
      assertEquals(2, report.units)
      assertEquals(2, report.staff)
      assertEquals(StaffRole.SUPERVISOR, report.role)
      assertTrue(orchestrator.isConnected())
      assertEquals("rk.k597.s3cret", vault.apiKeyForAuth())
      assertEquals("Ayesha", vault.staffIdentity()?.name)
      assertEquals("https://pms.demo13.com", conn.baseUrl()?.origin)
      assertEquals("Krishnachura Retreat", conn.propertyName())
      assertEquals(5_000L, conn.lastSyncedAt())
      assertEquals(1, db.roomTypeDao().getAll().size)
      assertEquals(2, db.roomUnitDao().count())
      assertEquals(2, db.staffRosterDao().getAll().size)
    }

  @Test
  fun `unknown staff name connects as housekeeper`() =
    runTest {
      api.bootstrapBody = BOOTSTRAP
      val report = orchestrator.validateAndConnect("https://pms.demo13.com", "rk.k1.s", "Newcomer").getOrThrow()
      assertEquals(StaffRole.HOUSEKEEPER, report.role)
      assertEquals(StaffRole.HOUSEKEEPER, vault.staffIdentity()?.role)
    }

  @Test
  fun `wrong key is refused cleanly with zero local trace`() =
    runTest {
      api.bootstrapError = PmsApiException("GET bootstrap failed", 401)
      val result = orchestrator.validateAndConnect("https://pms.demo13.com", "rk.k597.wrong", "Ayesha")

      assertTrue(result.isFailure)
      val error = (result.exceptionOrNull() as PmsException).error
      assertEquals(PmsError.WrongKey, error)
      assertEquals(R.string.setup_error_wrong_key, error.messageRes)
      assertFalse(orchestrator.isConnected())
      assertFalse(vaultHasKey())
      assertNull(conn.baseUrl())
      assertNull(conn.lastSyncedAt())
      assertEquals(0, db.roomUnitDao().count())
      assertEquals(0, db.roomTypeDao().getAll().size)
    }

  @Test
  fun `server failures map to server down and store nothing`() =
    runTest {
      api.bootstrapError = PmsApiException("GET bootstrap failed", 500)
      val down = orchestrator.validateAndConnect("https://pms.demo13.com", "rk.k1.s", "A")
      assertTrue((down.exceptionOrNull() as PmsException).error is PmsError.ServerDown)
      assertFalse(vaultHasKey())

      api.bootstrapError = PmsApiException("GET bootstrap failed", null, java.io.IOException("timeout"))
      val timeout = orchestrator.validateAndConnect("https://pms.demo13.com", "rk.k1.s", "A")
      assertTrue((timeout.exceptionOrNull() as PmsException).error is PmsError.ServerDown)
    }

  @Test
  fun `bad address is rejected before any network call`() =
    runTest {
      val result = orchestrator.validateAndConnect("http://pms.demo13.com", "rk.k1.s", "A")
      assertTrue((result.exceptionOrNull() as PmsException).error is PmsError.BadAddress)
    }

  @Test
  fun `bad key format and blank name are rejected locally`() =
    runTest {
      val badKey = orchestrator.validateAndConnect("https://pms.demo13.com", "nope", "A")
      assertEquals(PmsError.BadKeyFormat, (badKey.exceptionOrNull() as PmsException).error)

      val blankName = orchestrator.validateAndConnect("https://pms.demo13.com", "rk.k1.s", "  ")
      assertEquals(PmsError.BlankName, (blankName.exceptionOrNull() as PmsException).error)
    }

  @Test
  fun `contract mismatch is rejected with the server version`() =
    runTest {
      api.bootstrapBody = "{\"contractVersion\":7,\"units\":[]}"
      val result = orchestrator.validateAndConnect("https://pms.demo13.com", "rk.k1.s", "A")
      val error = result.exceptionOrNull() as PmsException
      assertEquals(PmsError.ContractMismatch(7), error.error)
      assertFalse(vaultHasKey())
    }

  @Test
  fun `sync now pulls the board and pushes the queue`() =
    runTest {
      api.bootstrapBody = BOOTSTRAP
      orchestrator.validateAndConnect("https://pms.demo13.com", "rk.k597.s3cret", "Ayesha").getOrThrow()
      db.syncQueueDao().enqueue(
        SyncMutationEntity(
          entityType = SyncEntityType.UNIT_STATUS,
          entityId = "u-101",
          op = SyncOp.UPSERT,
          payloadJson = SyncPayloads.unitStatus("u-101", HousekeepingState.CLEANED, "Ayesha", 1L, null, "too dark"),
          createdAt = 1L,
          dueAt = 1L,
          updatedAt = 1L,
        ),
      )

      val report = orchestrator.syncNow().getOrThrow()
      assertEquals(1, report.delivered)
      assertEquals(0, report.parked)
      assertEquals(0, report.pendingAfter)
      assertEquals(false, report.refreshed)
      val status = api.statusCalls.single()
      assertEquals("u-101", status.unitId)
      assertEquals(PmsHousekeeping.DIRTY, status.body.expected)
      assertEquals(StatusAction.MARK_CLEAN, status.body.action)
      assertEquals("Ayesha", status.body.staffName)
      assertEquals("too dark", status.body.note)
      assertEquals(5_000L, conn.lastSyncedAt())
    }

  @Test
  fun `sync pull 401 wipes the stored key and keeps the queue`() =
    runTest {
      api.bootstrapBody = BOOTSTRAP
      orchestrator.validateAndConnect("https://pms.demo13.com", "rk.k597.s3cret", "Ayesha").getOrThrow()
      db.syncQueueDao().enqueue(
        SyncMutationEntity(
          entityType = SyncEntityType.UNIT_STATUS,
          entityId = "u-101",
          op = SyncOp.UPSERT,
          payloadJson = SyncPayloads.unitStatus("u-101", HousekeepingState.CLEANED, "Ayesha", 1L),
          createdAt = 1L,
          dueAt = 1L,
          updatedAt = 1L,
        ),
      )
      api.bootstrapError = PmsApiException("GET bootstrap failed", 401)

      val result = orchestrator.syncNow()
      assertTrue(result.isFailure)
      assertEquals(PmsError.WrongKey, (result.exceptionOrNull() as PmsException).error)
      assertFalse(vaultHasKey())
      assertEquals("https://pms.demo13.com", conn.baseUrl()?.origin)
      assertEquals(1, db.syncQueueDao().pendingCount())
      assertFalse(orchestrator.isConnected())
    }

  @Test
  fun `drain 401 stops the drain wipes the key and keeps entries queued`() =
    runTest {
      api.bootstrapBody = BOOTSTRAP
      orchestrator.validateAndConnect("https://pms.demo13.com", "rk.k597.s3cret", "Ayesha").getOrThrow()
      repeat(2) {
        db.syncQueueDao().enqueue(
          SyncMutationEntity(
            entityType = SyncEntityType.UNIT_STATUS,
            entityId = "u-101",
            op = SyncOp.UPSERT,
            payloadJson = SyncPayloads.unitStatus("u-101", HousekeepingState.CLEANED, "Ayesha", 1L),
            createdAt = 1L,
            dueAt = 1L,
            updatedAt = 1L,
          ),
        )
      }
      api.callError = PmsApiException("POST room status failed", 401)

      val result = orchestrator.syncNow()
      assertTrue(result.isFailure)
      assertEquals(PmsError.WrongKey, (result.exceptionOrNull() as PmsException).error)
      assertFalse(vaultHasKey())
      assertEquals(2, db.syncQueueDao().pendingCount())
      assertEquals(1, api.statusAttempts)
      assertFalse(orchestrator.isConnected())
    }

  @Test
  fun `reconcile applies server done without queueing`() =
    runTest {
      api.bootstrapBody = BOOTSTRAP
      orchestrator.validateAndConnect("https://pms.demo13.com", "rk.k597.s3cret", "Ayesha").getOrThrow()
      val ticket =
        tickets.raiseTicket(
          "u-101",
          TicketCategory.PLUMBING,
          "leak",
          null,
          StaffMember("Ayesha", StaffRole.SUPERVISOR),
        ).getOrThrow()
      ticketIds.save(ticket.id, "srv-9")
      api.ticketLists =
        mapOf(
          "DONE" to
            "[{\"id\":\"srv-9\",\"unitId\":\"u-101\",\"title\":\"Plumbing\",\"status\":\"DONE\"," +
            "\"doneBy\":\"Rahim\",\"doneAt\":777}]",
        )

      val report = orchestrator.syncNow().getOrThrow()
      assertEquals(1, report.delivered)

      val reconciled = tickets.getTicket(ticket.id)!!
      assertEquals(TicketStatus.DONE, reconciled.status)
      assertEquals(777L, reconciled.updatedAtEpochMillis)
      assertTrue(api.updates.isEmpty())
      val doneEvent = db.activityDao().getRecent(10).single { it.type == ActivityType.TICKET_DONE }
      assertEquals("Rahim", doneEvent.actorName)
      assertEquals(777L, doneEvent.createdAt)
    }

  @Test
  fun `409 parks and schedules a refresh pull`() =
    runTest {
      api.bootstrapBody = BOOTSTRAP
      orchestrator.validateAndConnect("https://pms.demo13.com", "rk.k597.s3cret", "Ayesha").getOrThrow()
      db.syncQueueDao().enqueue(
        SyncMutationEntity(
          entityType = SyncEntityType.UNIT_STATUS,
          entityId = "u-101",
          op = SyncOp.UPSERT,
          payloadJson = SyncPayloads.unitStatus("u-101", HousekeepingState.CLEANED, "Ayesha", 1L),
          createdAt = 1L,
          dueAt = 1L,
          updatedAt = 1L,
        ),
      )
      api.callError = PmsApiException("POST room status failed", 409)

      val report = orchestrator.syncNow().getOrThrow()
      assertEquals(1, report.parked)
      assertEquals(true, report.refreshed)
      assertEquals(3, api.bootstrapCalls)
      assertTrue(
        db.activityDao().getRecent(10).any {
          it.type == ActivityType.PUSH_PARKED &&
            (it.detail ?: "").contains("HTTP 409")
        },
      )
    }

  private fun vaultHasKey(): Boolean = vault.hasApiKey()

  private class FakeClock : Clock {
    override fun nowEpochMillis(): Long = 5_000L
  }

  companion object {
    private const val BOOTSTRAP =
      """{"contractVersion":1,
        "property":{"id":"demo13","name":"Krishnachura Retreat"},
        "roomTypes":[{"id":"rt-terra","code":"TERRA","nameEn":"Terracotta Villa","nameBn":"টেরাকোটা ভিলা","bedSummary":"1 king","baseOccupancy":2}],
        "units":[
          {"id":"u-101","number":"101","roomTypeId":"rt-terra","floor":"1","occupancy":"VACANT","housekeeping":"DIRTY","housekeepingAt":"2026-11-15T08:00:00Z"},
          {"id":"u-102","number":"102","roomTypeId":"rt-terra","floor":"1","occupancy":"OCCUPIED","housekeeping":"CLEAN"}],
        "staff":[{"name":"Ayesha","role":"SUPERVISOR"},{"name":"Karim","role":"HOUSEKEEPER"}],
        "checklist":["bed_linen","vacuum_mop"]}"""
  }
}
