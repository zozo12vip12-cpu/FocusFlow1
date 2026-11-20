package app.focusflow.sound

import app.focusflow.data.Sound
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin

/**
 * Generates calm background noise sample by sample. Because the sound is produced in code,
 * the app contains no audio files (so there are no copyright or licensing questions).
 */
class NoiseGen(sampleRate: Int) {
    private val rnd = Random()

    // pink noise filter state (Paul Kellet's economy filter)
    private var b0 = 0f
    private var b1 = 0f
    private var b2 = 0f

    private var brown = 0f
    private var prevWhite = 0f
    private var drop = 0f
    private var lfo = 0.0
    private val lfoStep = 2.0 * PI * 0.12 / sampleRate

    fun fill(out: ShortArray, sound: Sound) {
        for (i in out.indices) {
            val white = rnd.nextFloat() * 2f - 1f
            val v: Float = when (sound) {
                Sound.WHITE -> white * 0.30f
                Sound.BROWN -> {
                    brown = (brown + 0.02f * white) / 1.02f
                    brown * 3.5f
                }
                Sound.WIND -> {
                    brown = (brown + 0.02f * white) / 1.02f
                    lfo += lfoStep
                    brown * 3.5f * (0.55f + 0.45f * sin(lfo).toFloat())
                }
                Sound.RAIN -> {
                    b0 = 0.99765f * b0 + white * 0.0990460f
                    b1 = 0.96300f * b1 + white * 0.2965164f
                    b2 = 0.57000f * b2 + white * 1.0526913f
                    val pink = (b0 + b1 + b2 + white * 0.1848f) * 0.12f
                    val hiss = (white - prevWhite) * 0.12f
                    prevWhite = white
                    // now and then a louder "drop" that fades out quickly
                    if (rnd.nextInt(7000) == 0) drop = 0.5f + 0.5f * rnd.nextFloat()
                    drop *= 0.996f
                    pink + hiss + drop * (rnd.nextFloat() * 2f - 1f) * 0.25f
                }
            }
            val clipped = if (v > 1f) 1f else if (v < -1f) -1f else v
            out[i] = (clipped * 32767f * 0.5f).toInt().toShort()
        }
    }
}
