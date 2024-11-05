package mobappdev.example.nback_cimpl.services

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import mobappdev.example.nback_cimpl.R



class AudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    /**
     * Plays the audio corresponding to the given event value.
     * Releases any previously playing audio before starting new playback.
     */
    fun playAudio(eventValue: Int) {
        release()  // Release any existing mediaPlayer before creating a new one

        // Select the appropriate audio resource based on eventValue
        val audioResId = when (eventValue) {
            1 -> R.raw.audio_a
            2 -> R.raw.audio_b
            3 -> R.raw.audio_c
            4 -> R.raw.audio_d
            5 -> R.raw.audio_e
            6 -> R.raw.audio_f
            7 -> R.raw.audio_g
            8 -> R.raw.audio_h
            9 -> R.raw.audio_i
            else -> null
        }

        // Play the selected audio file if it exists
        if (audioResId != null) {
            mediaPlayer = MediaPlayer.create(context, audioResId).apply {
                setOnCompletionListener {
                    release()  // Automatically release when audio finishes
                }
                start()  // Start playback
            }
        }
    }

    /**
     * Releases the MediaPlayer if it's currently active, freeing up resources.
     */
    fun release() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
                Log.d("AudioPlayer", "MediaPlayer released")
            } catch (e: IllegalStateException) {
                Log.e("AudioPlayer", "MediaPlayer release error: ${e.message}")
            } finally {
                mediaPlayer = null
            }
        }
    }
}
