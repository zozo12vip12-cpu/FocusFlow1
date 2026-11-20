package app.focusflow.data

import androidx.annotation.StringRes
import app.focusflow.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class Achievement(
    val id: String,
    @StringRes val title: Int,
    @StringRes val desc: Int,
    val current: Int,
    val target: Int
) {
    val unlocked: Boolean get() = current >= target
    val progress: Float get() = (current.toFloat() / target).coerceIn(0f, 1f)
}

data class StatsData(
    val todayMin: Int = 0,
    val todaySessions: Int = 0,
    val weekMin: Int = 0,
    val totalMin: Int = 0,
    val totalSessions: Int = 0,
    val streak: Int = 0,
    /** Focus minutes for the last 7 days, oldest first (the last item is today). */
    val last7Min: List<Int> = List(7) { 0 },
    val avgDistractions: Float = 0f,
    val achievements: List<Achievement> = emptyList()
)

/** Pure functions that turn the list of sessions into the numbers shown in the UI. */
object StatsCalc {

    fun compute(all: List<Session>, dailyGoal: Int): StatsData {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        fun dayOf(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
        fun hourOf(ms: Long): Int = Instant.ofEpochMilli(ms).atZone(zone).hour

        val byDay: Map<LocalDate, List<Session>> = all.groupBy { dayOf(it.endedAt) }
        val minutesByDay: Map<LocalDate, Int> =
            byDay.mapValues { entry -> entry.value.sumOf { it.durationSec } / 60 }

        val last7: List<Int> = (6 downTo 0).map { back ->
            minutesByDay[today.minusDays(back.toLong())] ?: 0
        }

        // Current streak: consecutive days with at least one session, ending today
        // (or yesterday, so the streak is not lost before you start today's first session).
        var streak = 0
        var cursor = if (byDay.containsKey(today)) today else today.minusDays(1)
        while (byDay.containsKey(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }

        // Best streak ever.
        var bestStreak = 0
        var run = 0
        var previous: LocalDate? = null
        for (day in byDay.keys.sorted()) {
            val p = previous
            run = if (p != null && p.plusDays(1) == day) run + 1 else 1
            if (run > bestStreak) bestStreak = run
            previous = day
        }

        val bestDaySessions = byDay.values.maxOfOrNull { it.size } ?: 0
        val goalDays = byDay.values.count { it.size >= dailyGoal }

        var cleanRun = 0
        var bestClean = 0
        for (s in all) {
            if (s.distractions == 0) {
                cleanRun++
                if (cleanRun > bestClean) bestClean = cleanRun
            } else {
                cleanRun = 0
            }
        }

        val early = all.count { hourOf(it.startedAt) < 8 }
        val night = all.count { hourOf(it.startedAt) >= 22 }
        val totalMin = all.sumOf { it.durationSec } / 60
        val avgDistractions =
            if (all.isEmpty()) 0f else all.sumOf { it.distractions }.toFloat() / all.size
        val todayList: List<Session> = byDay[today].orEmpty()

        val achievements = listOf(
            Achievement("first", R.string.ach_first, R.string.ach_first_d, all.size, 1),
            Achievement("ten", R.string.ach_ten, R.string.ach_ten_d, all.size, 10),
            Achievement("warm", R.string.ach_warm, R.string.ach_warm_d, bestDaySessions, 4),
            Achievement("marathon", R.string.ach_marathon, R.string.ach_marathon_d, bestDaySessions, 8),
            Achievement("streak3", R.string.ach_streak3, R.string.ach_streak3_d, bestStreak, 3),
            Achievement("streak7", R.string.ach_streak7, R.string.ach_streak7_d, bestStreak, 7),
            Achievement("h10", R.string.ach_h10, R.string.ach_h10_d, totalMin / 60, 10),
            Achievement("h50", R.string.ach_h50, R.string.ach_h50_d, totalMin / 60, 50),
            Achievement("calm", R.string.ach_calm, R.string.ach_calm_d, bestClean, 5),
            Achievement("goal", R.string.ach_goal, R.string.ach_goal_d, goalDays, 1),
            Achievement("early", R.string.ach_early, R.string.ach_early_d, early, 1),
            Achievement("night", R.string.ach_night, R.string.ach_night_d, night, 1)
        )

        return StatsData(
            todayMin = minutesByDay[today] ?: 0,
            todaySessions = todayList.size,
            weekMin = last7.sum(),
            totalMin = totalMin,
            totalSessions = all.size,
            streak = streak,
            last7Min = last7,
            avgDistractions = avgDistractions,
            achievements = achievements
        )
    }
}
