package com.vivereply.roomready.data.repository

import com.vivereply.roomready.data.Clock
import com.vivereply.roomready.data.local.ActivityDao
import com.vivereply.roomready.data.local.ActivityEntity
import com.vivereply.roomready.data.local.ChecklistDao
import com.vivereply.roomready.data.local.ChecklistStepEntity
import com.vivereply.roomready.data.local.RoomTypeDao
import com.vivereply.roomready.data.local.RoomTypeEntity
import com.vivereply.roomready.data.local.RoomUnitDao
import com.vivereply.roomready.data.local.RoomUnitEntity
import com.vivereply.roomready.data.local.StaffRosterDao
import com.vivereply.roomready.data.local.StaffRosterEntity
import com.vivereply.roomready.data.local.SyncMutationEntity
import com.vivereply.roomready.data.local.SyncQueueDao
import com.vivereply.roomready.data.sync.SyncEntityType
import com.vivereply.roomready.data.sync.SyncOp
import com.vivereply.roomready.data.sync.SyncPayloads
import com.vivereply.roomready.domain.model.ActivityType
import com.vivereply.roomready.domain.model.ChecklistProgress
import com.vivereply.roomready.domain.model.HousekeepingState
import com.vivereply.roomready.domain.model.PmsHousekeeping
import com.vivereply.roomready.domain.model.RoomType
import com.vivereply.roomready.domain.model.RoomUnit
import com.vivereply.roomready.domain.model.StaffMember
import com.vivereply.roomready.domain.model.TurnoverStep
import com.vivereply.roomready.domain.state.HousekeepingStateMachine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Local-action errors for room operations (typed data, no user copy). */
sealed interface RoomActionError {
  data class UnknownUnit(val unitId: String) : RoomActionError

  /** Checklist steps can only change while the room is being cleaned. */
  data class StepsLocked(val hkState: HousekeepingState) : RoomActionError
}

class RoomActionException(val error: RoomActionError) : IllegalStateException(error.toString())

data class BootstrapImportResult(
  val types: Int,
  val units: Int,
  val hkPreserved: Int,
)

/**
 * Room units, checklist progress, and the PMS bootstrap import.
 *
 * Every local state change is validated by [HousekeepingStateMachine] first,
 * so illegal transitions are impossible here — not just hidden in UI.
 * Every mutation also writes an activity entry and a sync-queue entry, so it
 * works fully offline and syncs later. Occupancy is never touched here.
 */
class RoomRepository(
  private val units: RoomUnitDao,
  private val types: RoomTypeDao,
  private val roster: StaffRosterDao,
  private val steps: ChecklistDao,
  private val activity: ActivityDao,
  private val queue: SyncQueueDao,
  private val clock: Clock,
) {

  fun observeUnits(): Flow<List<RoomUnit>> =
    units.observeAll().map { list -> list.map(RoomUnitEntity::toDomain) }

  suspend fun getUnit(unitId: String): RoomUnit? = units.getById(unitId)?.toDomain()

  suspend fun roomTypes(): List<RoomType> = types.getAll().map(RoomTypeEntity::toDomain)

  suspend fun progress(unitId: String): ChecklistProgress =
    ChecklistProgress(
      unitId = unitId,
      done = steps.getForUnit(unitId).map { it.step }.toSet(),
    )

  /** HOUSEKEEPER: Dirty -> Cleaning. */
  suspend fun startCleaning(unitId: String, actor: StaffMember): Result<RoomUnit> {
    val unit = units.getById(unitId) ?: return unknownUnit(unitId)
    val now = clock.nowEpochMillis()
    val next =
      HousekeepingStateMachine.evaluate(unit.hkState, HousekeepingState.CLEANING, actor.role)
        .getOrElse { return Result.failure(it) }
    units.updateHkState(unitId, next, now)
    logStatusChange(unit, next, actor, now, detail = null)
    enqueueStatus(unit, next, actor, now)
    return Result.success(unit.toDomain().copy(hkState = next, updatedAtEpochMillis = now))
  }

  /**
   * HOUSEKEEPER: tick/untick one checklist step. Only while CLEANING.
   * Completing the final step logs CHECKLIST_COMPLETED.
   */
  suspend fun setStepDone(
    unitId: String,
    step: TurnoverStep,
    done: Boolean,
    actor: StaffMember,
  ): Result<ChecklistProgress> {
    val unit = units.getById(unitId) ?: return unknownUnit(unitId)
    if (unit.hkState != HousekeepingState.CLEANING) {
      return Result.failure(RoomActionException(RoomActionError.StepsLocked(unit.hkState)))
    }
    val now = clock.nowEpochMillis()
    if (done) {
      steps.upsert(ChecklistStepEntity(unitId, step, now, actor.name))
    } else {
      steps.removeStep(unitId, step)
    }
    val current = progress(unitId)
    queue.enqueue(
      SyncMutationEntity(
        entityType = SyncEntityType.CHECKLIST,
        entityId = unitId,
        op = SyncOp.UPSERT,
        payloadJson =
          SyncPayloads.checklist(
            unitId,
            current.done.sortedBy { it.order }.map { it.key },
            current.isComplete,
            actor.name,
            now,
          ),
        createdAt = now,
        dueAt = now,
        updatedAt = now,
      ),
    )
    if (done && current.isComplete) {
      activity.insert(
        ActivityEntity(
          type = ActivityType.CHECKLIST_COMPLETED,
          actorName = actor.name,
          role = actor.role,
          unitId = unitId,
          roomNumber = unit.number,
          detail = null,
          createdAt = now,
        ),
      )
    }
    return Result.success(current)
  }

  /** HOUSEKEEPER: Cleaning -> Cleaned. Requires all 8 steps done. */
  suspend fun finishCleaning(unitId: String, actor: StaffMember): Result<RoomUnit> {
    val unit = units.getById(unitId) ?: return unknownUnit(unitId)
    val current = progress(unitId)
    val now = clock.nowEpochMillis()
    val next =
      HousekeepingStateMachine.evaluate(
        unit.hkState,
        HousekeepingState.CLEANED,
        actor.role,
        checklistDone = current.doneCount,
        checklistTotal = current.total,
      ).getOrElse { return Result.failure(it) }
    units.updateHkState(unitId, next, now)
    logStatusChange(unit, next, actor, now, detail = null)
    enqueueStatus(unit, next, actor, now)
    return Result.success(unit.toDomain().copy(hkState = next, updatedAtEpochMillis = now))
  }

  /** SUPERVISOR: Cleaned -> Ready. */
  suspend fun inspect(unitId: String, actor: StaffMember): Result<RoomUnit> {
    val unit = units.getById(unitId) ?: return unknownUnit(unitId)
    val now = clock.nowEpochMillis()
    val next =
      HousekeepingStateMachine.evaluate(unit.hkState, HousekeepingState.READY, actor.role)
        .getOrElse { return Result.failure(it) }
    units.updateHkState(unitId, next, now)
    logStatusChange(unit, next, actor, now, detail = null)
    enqueueStatus(unit, next, actor, now)
    return Result.success(unit.toDomain().copy(hkState = next, updatedAtEpochMillis = now))
  }

  /**
   * SUPERVISOR: Cleaned -> Dirty with a reason. Checklist progress is cleared
   * so the redo starts fresh; a housekeeper cannot finish without re-doing
   * all 8 steps.
   */
  suspend fun sendBack(unitId: String, actor: StaffMember, reason: String): Result<RoomUnit> {
    val unit = units.getById(unitId) ?: return unknownUnit(unitId)
    val now = clock.nowEpochMillis()
    val next =
      HousekeepingStateMachine.evaluate(
        unit.hkState,
        HousekeepingState.DIRTY,
        actor.role,
        reason = reason,
      ).getOrElse { return Result.failure(it) }
    units.updateHkState(unitId, next, now)
    steps.clearUnit(unitId)
    activity.insert(
      ActivityEntity(
        type = ActivityType.SENT_BACK,
        actorName = actor.name,
        role = actor.role,
        unitId = unitId,
        roomNumber = unit.number,
        detail = reason.trim(),
        createdAt = now,
      ),
    )
    enqueueStatus(unit, next, actor, now)
    return Result.success(unit.toDomain().copy(hkState = next, updatedAtEpochMillis = now))
  }

  /**
   * Upserts PMS bootstrap inventory (the only source of room types/units).
   *
   * Occupancy and PMS-known fields always come from the import. Local hk
   * state is preserved when it still maps to the same PMS code (notably
   * CLEANING, which the PMS reports as DIRTY) or when unsynced local
   * mutations exist for the unit; otherwise the PMS state wins. When local
   * state is preserved, its entry timestamps are kept too, so a re-import
   * never rewinds the time-in-state clock.
   */
  suspend fun importBootstrap(
    roomTypes: List<RoomType>,
    roomUnits: List<RoomUnit>,
    actorName: String = SYSTEM_ACTOR,
  ): BootstrapImportResult {
    types.upsertAll(roomTypes.map(RoomTypeEntity::fromDomain))
    var preserved = 0
    for (incoming in roomUnits) {
      val existing = units.getById(incoming.id)
      if (existing == null) {
        units.upsert(RoomUnitEntity.fromDomain(incoming))
        continue
      }
      val keepLocal =
        PmsHousekeeping.fromLocal(existing.hkState) == PmsHousekeeping.fromLocal(incoming.hkState) ||
          queue.unsyncedCountFor(SyncEntityType.UNIT_STATUS, incoming.id) > 0
      if (keepLocal) preserved++
      val merged =
        if (keepLocal) {
          incoming.copy(
            hkState = existing.hkState,
            updatedAtEpochMillis = existing.updatedAt,
            housekeepingAtEpochMillis = existing.housekeepingAt,
          )
        } else {
          incoming
        }
      units.upsert(RoomUnitEntity.fromDomain(merged))
    }
    val now = clock.nowEpochMillis()
    activity.insert(
      ActivityEntity(
        type = ActivityType.BOOTSTRAP_IMPORTED,
        actorName = actorName,
        role = null,
        unitId = null,
        roomNumber = null,
        detail = "${roomTypes.size}/${roomUnits.size}/$preserved",
        createdAt = now,
      ),
    )
    return BootstrapImportResult(roomTypes.size, roomUnits.size, preserved)
  }

  /** Replaces the staff roster snapshot from a bootstrap import. */
  suspend fun importRoster(staff: List<StaffMember>) {
    roster.clear()
    if (staff.isNotEmpty()) roster.upsertAll(staff.map(StaffRosterEntity::fromDomain))
  }

  suspend fun roster(): List<StaffMember> = roster.getAll().map(StaffRosterEntity::toDomain)

  private suspend fun logStatusChange(
    unit: RoomUnitEntity,
    next: HousekeepingState,
    actor: StaffMember,
    now: Long,
    detail: String?,
  ) {
    activity.insert(
      ActivityEntity(
        type = ActivityType.STATUS_CHANGED,
        actorName = actor.name,
        role = actor.role,
        unitId = unit.id,
        roomNumber = unit.number,
        detail = detail ?: "${unit.hkState.name}>${next.name}",
        createdAt = now,
      ),
    )
  }

  private suspend fun enqueueStatus(
    unit: RoomUnitEntity,
    next: HousekeepingState,
    actor: StaffMember,
    now: Long,
  ) {
    queue.enqueue(
      SyncMutationEntity(
        entityType = SyncEntityType.UNIT_STATUS,
        entityId = unit.id,
        op = SyncOp.UPSERT,
        payloadJson = SyncPayloads.unitStatus(unit.id, next, actor.name, now),
        createdAt = now,
        dueAt = now,
        updatedAt = now,
      ),
    )
  }

  private fun <T> unknownUnit(unitId: String): Result<T> =
    Result.failure(RoomActionException(RoomActionError.UnknownUnit(unitId)))

  companion object {
    const val SYSTEM_ACTOR: String = "system"
  }
}
