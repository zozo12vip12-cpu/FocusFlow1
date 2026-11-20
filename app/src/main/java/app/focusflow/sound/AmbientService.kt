package app.focusflow.sound

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.focusflow.R
import app.focusflow.data.Sound
import app.focusflow.timer.Notifier

/**
 * Plays the focus sound. It is a foreground service of type "mediaPlayback" so the sound keeps
 * playing with the screen off. It is only started when the user taps a sound in the app.
 */
class AmbientService : Service() {

    private var track: AudioTrack? = null
    private var worker: Thread? = null

    @Volatile
    private var running = false

    @Volatile
    private var sound: Sound = Sound.RAIN

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val requested = Sound.fromKey(intent?.getStringExtra(EXTRA_SOUND)) ?: Sound.RAIN
        try {
            goForeground(requested)
            sound = requested
            active = requested
            if (!running) startPlayback()
        } catch (e: Exception) {
            // Could not start in the foreground or could not open the audio output.
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun goForeground(s: Sound) {
        Notifier.ensureChannels(this)
        val stop = PendingIntent.getService(
            this,
            0,
            Intent(this, AmbientService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, Notifier.CH_SOUND)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(getString(R.string.notif_sound_title))
            .setContentText(getString(s.label))
            .setContentIntent(Notifier.openAppIntent(this))
            .setOngoing(true)
            .setSilent(true)
            .addAction(0, getString(R.string.stop), stop)
            .build()
        ServiceCompat.startForeground(
            this,
            Notifier.ID_SOUND,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
    }

    private fun startPlayback() {
        val rate = 22050
        val minBuffer = AudioTrack.getMinBufferSize(
            rate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferBytes = maxOf(minBuffer, 4096) * 2
        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track = t
        t.play()
        running = true
        val thread = Thread {
            val generator = NoiseGen(rate)
            val buffer = ShortArray(2048)
            while (running) {
                generator.fill(buffer, sound)
                t.write(buffer, 0, buffer.size)
            }
        }
        worker = thread
        thread.start()
    }

    private fun stopPlayback() {
        running = false
        try {
            worker?.join(500)
        } catch (e: InterruptedException) {
            // ignore: we are shutting down anyway
        }
        worker = null
        val t = track
        if (t != null) {
            try {
                t.stop()
            } catch (e: IllegalStateException) {
                // already stopped
            }
            t.release()
        }
        track = null
    }

    override fun onDestroy() {
        stopPlayback()
        active = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "app.focusflow.action.STOP_SOUND"
        private const val EXTRA_SOUND = "sound"

        /** The sound that is playing right now, or null. Read by the UI. */
        @Volatile
        var active: Sound? = null
            private set

        fun start(ctx: Context, s: Sound) {
            val intent = Intent(ctx, AmbientService::class.java).putExtra(EXTRA_SOUND, s.key)
            ContextCompat.startForegroundService(ctx, intent)
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, AmbientService::class.java))
        }
    }
}
