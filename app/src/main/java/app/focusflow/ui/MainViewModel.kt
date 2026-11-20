package app.focusflow.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.focusflow.data.AppSettings
import app.focusflow.data.Phase
import app.focusflow.data.Prefs
import app.focusflow.data.RunStatus
import app.focusflow.data.SessionDb
import app.focusflow.data.Sound
import app.focusflow.data.StatsCalc
import app.focusflow.data.StatsData
import app.focusflow.data.TimerState
import app.focusflow.sound.AmbientService
import app.focusflow.timer.Alarms
import app.focusflow.timer.TimerController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the screens need to draw themselves. */
data class UiState(
    val timer: TimerState,
    val settings: AppSettings,
    val remainingMs: Long,
    val totalMs: Long,
    val sound: Sound?,
    val exactAlarmOk: Boolean
) {
    /** 0f..1f: how much of the current phase has already passed. */
    val progress: Float
        get() = if (totalMs <= 0L) 0f
        else (1f - remainingMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx = app.applicationContext
    private val prefs = Prefs.get(ctx)

    /** Used to refresh the UI immediately after a button press (instead of waiting for the next tick). */
    private val pokes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val _stats = MutableStateFlow(StatsCalc.compute(emptyList(), prefs.settings.dailyGoal))
    val stats: StateFlow<StatsData> = _stats.asStateFlow()

    init {
        refreshStats()
    }

    /**
     * Ticks 4 times per second, but only while a screen is collecting it (the app is visible).
     * When the countdown reaches zero while the app is open, the session is completed here.
     */
    val ui: StateFlow<UiState> = merge(
        flow {
            while (true) {
                emit(Unit)
                delay(250)
            }
        },
        pokes
    ).map {
        val st = prefs.timer
        if (st.status == RunStatus.RUNNING && System.currentTimeMillis() >= st.endAt) {
            TimerController.complete(ctx)
            refreshStats()
        }
        snapshot()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), snapshot())

    private fun snapshot(): UiState {
        val settings = prefs.settings
        val st = prefs.timer
        val now = System.currentTimeMillis()
        val (total, remaining) = when (st.status) {
            RunStatus.RUNNING -> st.totalMs to (st.endAt - now).coerceAtLeast(0L)
            RunStatus.PAUSED -> st.totalMs to st.remainingMs
            RunStatus.IDLE -> {
                val d = TimerController.durationMs(st.phase, settings)
                d to d
            }
        }
        return UiState(
            timer = st,
            settings = settings,
            remainingMs = remaining,
            totalMs = total,
            sound = AmbientService.active,
            exactAlarmOk = Alarms.canScheduleExact(ctx)
        )
    }

    fun refreshStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val all = SessionDb.get(ctx).all()
            _stats.value = StatsCalc.compute(all, prefs.settings.dailyGoal)
        }
    }

    fun start() {
        TimerController.startOrResume(ctx)
        pokes.tryEmit(Unit)
    }

    fun pause() {
        TimerController.pause(ctx)
        pokes.tryEmit(Unit)
    }

    fun reset() {
        TimerController.reset(ctx)
        pokes.tryEmit(Unit)
    }

    fun skip() {
        TimerController.skip(ctx)
        pokes.tryEmit(Unit)
    }

    fun setPhase(phase: Phase) {
        TimerController.setPhase(ctx, phase)
        pokes.tryEmit(Unit)
    }

    fun setSound(sound: Sound?) {
        if (sound == null) {
            AmbientService.stop(ctx)
        } else {
            AmbientService.start(ctx, sound)
        }
    }

    fun update(change: (AppSettings) -> AppSettings) {
        prefs.settings = change(prefs.settings)
        pokes.tryEmit(Unit)
        refreshStats() // the daily goal may have changed
    }

    fun wipe() {
        TimerController.wipe(ctx)
        refreshStats()
        pokes.tryEmit(Unit)
    }
}
