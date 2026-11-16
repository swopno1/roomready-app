package com.vivereply.roomready.data.repository

import com.vivereply.roomready.data.local.ActivityDao
import com.vivereply.roomready.data.local.ActivityEntity
import com.vivereply.roomready.domain.model.ActivityEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Read API over the offline activity log (writes happen in repositories). */
class ActivityRepository(private val activity: ActivityDao) {

  fun observeRecent(limit: Int = DEFAULT_LIMIT): Flow<List<ActivityEvent>> =
    activity.observeRecent(limit).map { list -> list.map(ActivityEntity::toDomain) }

  suspend fun recent(limit: Int = DEFAULT_LIMIT): List<ActivityEvent> =
    activity.getRecent(limit).map(ActivityEntity::toDomain)

  companion object {
    const val DEFAULT_LIMIT: Int = 100
  }
}
