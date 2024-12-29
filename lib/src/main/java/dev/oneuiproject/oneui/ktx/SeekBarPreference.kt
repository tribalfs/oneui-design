package dev.oneuiproject.oneui.ktx

import androidx.appcompat.widget.SeslSeekBar
import androidx.preference.SeekBarPreference

/**
 * Sets listeners to handle user interactions with this [SeekBarPreference].
 *
 * @param onProgress Lambda function to be invoked when the progress of the seek bar changes.
 *                  Receives the seek bar, progress value, and whether the change was initiated by user.
 * @param onStart (Optional) Lambda function to be invoked when the user starts sliding the seek bar.
 *               Receives the seek bar instance.
 * @param onStop (Optional) Lambda function to be invoked when the user stops sliding the seek bar.
 *              Receives the seek bar instance.
 *
 * Example usage:
 * ```kotlin
 * seekBarPreference.setSeekBarChangeListeners(
 *     onProgress = { seekBar, progress, fromUser ->
 *         // Handle progress change
 *     },
 *     onStart = { seslSeekBar ->
 *         // Handle start of slide
 *     },
 *     onStop = { seslSeekBar ->
 *         // Handle end of slide
 *     }
 * )
 * ```
 */
inline fun SeekBarPreference.setSeekBarChangeListeners(
    crossinline onProgress: (seekBar: SeslSeekBar?, progress: Int, fromUser: Boolean) -> Unit,
    crossinline onStart: (seslSeekBar: SeslSeekBar?) -> Unit = {},
    crossinline onStop: (seslSeekBar: SeslSeekBar?) -> Unit = {}
) {
    val onSeekBarPreferenceChangeListener = object :
        SeekBarPreference.OnSeekBarPreferenceChangeListener {
        override fun onProgressChanged(
            seekBar: SeslSeekBar?,
            progress: Int,
            fromUser: Boolean
        ) {
            onProgress.invoke(seekBar, progress, fromUser)
        }

        override fun onStartTrackingTouch(seslSeekBar: SeslSeekBar?) {
            onStart.invoke(seslSeekBar)
        }

        override fun onStopTrackingTouch(seslSeekBar: SeslSeekBar?) {
            onStop.invoke(seslSeekBar)
        }

    }
    setOnSeekBarPreferenceChangeListener(onSeekBarPreferenceChangeListener)
}
