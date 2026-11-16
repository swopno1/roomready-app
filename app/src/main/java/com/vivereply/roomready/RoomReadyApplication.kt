package com.vivereply.roomready

import android.app.Application
import com.vivereply.roomready.data.EngineGraph

class RoomReadyApplication : Application() {
  val graph: EngineGraph by lazy { EngineGraph(this) }
}
