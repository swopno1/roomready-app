package com.vivereply.roomready.data.pms

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vivereply.roomready.R
import com.vivereply.roomready.data.Clock
import com.vivereply.roomready.data.local.RoomReadyDatabase
import com.vivereply.roomready.data.local.SyncMutationEntity
import com.vivereply.roomready.data.pms.PmsOutboundTransportTest.FakePmsApi
import com.vivereply.roomready.data.pms.PmsOutboundTransportTest.FakeStore
import com.vivereply.roomready.data.pms.PmsOutboundTransportTest.FakeVault
import com.vivereply.roomready.data.repository.RoomRepository
import com.vivereply.roomready.data.sync.SyncEntityType
import com.vivereply.roomready.data.sync.SyncOp
import com.vivereply.roomready.domain.model.StaffRole
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
  private lateinit var api: FakePmsApi
  private lateinit var vault: FakeVault
  private lateinit var conn: FakeStore
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
    orchestrator = SyncOrchestrator(api, vault, conn, rooms, db.syncQueueDao(), db.activityDao(), FakeClock())
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
          payloadJson = "{}",
          createdAt = 1L,
          dueAt = 1L,
          updatedAt = 1L,
        ),
      )

      val report = orchestrator.syncNow().getOrThrow()
      assertEquals(1, report.delivered)
      assertEquals(0, report.parked)
      assertEquals(0, report.pendingAfter)
      assertEquals("app:k597:Ayesha", api.pushed.single().actor)
      assertEquals(5_000L, conn.lastSyncedAt())
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
