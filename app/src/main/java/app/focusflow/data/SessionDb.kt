package app.focusflow.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Local SQLite database. One row per completed focus session. Nothing ever leaves the device. */
class SessionDb private constructor(ctx: Context) : SQLiteOpenHelper(ctx, "focus.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE sessions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "started_at INTEGER NOT NULL, " +
                "ended_at INTEGER NOT NULL, " +
                "duration_sec INTEGER NOT NULL, " +
                "distractions INTEGER NOT NULL DEFAULT 0)"
        )
        db.execSQL("CREATE INDEX idx_sessions_ended ON sessions(ended_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // No migrations yet. When you change the schema, bump the version above and migrate here.
    }

    fun insert(s: Session) {
        val values = ContentValues()
        values.put("started_at", s.startedAt)
        values.put("ended_at", s.endedAt)
        values.put("duration_sec", s.durationSec)
        values.put("distractions", s.distractions)
        writableDatabase.insert("sessions", null, values)
    }

    /** All sessions, oldest first. A few thousand rows are fine to load in one go. */
    fun all(): List<Session> {
        val out = ArrayList<Session>()
        val sql = "SELECT id, started_at, ended_at, duration_sec, distractions FROM sessions ORDER BY ended_at ASC"
        readableDatabase.rawQuery(sql, null).use { c ->
            while (c.moveToNext()) {
                out.add(Session(c.getLong(0), c.getLong(1), c.getLong(2), c.getInt(3), c.getInt(4)))
            }
        }
        return out
    }

    fun clear() {
        writableDatabase.delete("sessions", null, null)
    }

    companion object {
        @Volatile
        private var instance: SessionDb? = null

        fun get(ctx: Context): SessionDb = instance ?: synchronized(this) {
            instance ?: SessionDb(ctx.applicationContext).also { instance = it }
        }
    }
}
