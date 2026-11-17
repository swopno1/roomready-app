package com.vivereply.roomready.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vivereply.roomready.data.EngineGraph

/** Builds the tab ViewModels from the app graph (no DI framework). */
class GraphViewModelFactory(private val graph: EngineGraph) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T =
    when {
      modelClass.isAssignableFrom(ConnectionViewModel::class.java) ->
        ConnectionViewModel(graph.orchestrator) as T
      modelClass.isAssignableFrom(SetupViewModel::class.java) ->
        SetupViewModel(graph.orchestrator) as T
      modelClass.isAssignableFrom(BoardViewModel::class.java) ->
        BoardViewModel(graph.rooms, graph.orchestrator) as T
      modelClass.isAssignableFrom(ActivityFeedViewModel::class.java) ->
        ActivityFeedViewModel(graph.activity, graph.database.syncQueueDao()) as T
      modelClass.isAssignableFrom(TicketsViewModel::class.java) ->
        TicketsViewModel(graph.tickets, graph.orchestrator, graph.photos) as T
      else -> throw IllegalArgumentException("Unknown ViewModel ${modelClass.name}")
    }
}
