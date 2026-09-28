package com.pawansingh.vp.models

import android.app.Dialog
import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import com.pawansingh.vp.R
import kotlin.math.ceil

class VolumeDialog : AppCompatDialogFragment() {

    private var cross: ImageView? = null
    private var volume_no: TextView? = null
    private var seekBar: SeekBar? = null

    var audioManager: AudioManager? = null

    override fun onCreateDialog(
        savedInstanceState: Bundle?
    ): Dialog {

        val activity = requireActivity()

        val builder =
            AlertDialog.Builder(activity)

        val inflater =
            activity.layoutInflater

        val view =
            inflater.inflate(
                R.layout.vol_dialog_item,
                null
            )

        builder.setView(view)

        activity.setVolumeControlStream(
            AudioManager.STREAM_MUSIC
        )

        cross =
            view.findViewById(R.id.vol_close)

        volume_no =
            view.findViewById(R.id.vol_number)

        seekBar =
            view.findViewById(R.id.vol_seekBar)

        audioManager =
            requireContext().getSystemService(
                Context.AUDIO_SERVICE
            ) as AudioManager

        val audio = audioManager ?: return builder.create()

        seekBar?.max =
            audio.getStreamMaxVolume(
                AudioManager.STREAM_MUSIC
            )

        seekBar?.progress =
            audio.getStreamVolume(
                AudioManager.STREAM_MUSIC
            )

        seekBar?.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {

                    audio.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        progress,
                        0
                    )

                    val mediaVolume =
                        audio.getStreamVolume(
                            AudioManager.STREAM_MUSIC
                        )

                    val maxVolume =
                        audio.getStreamMaxVolume(
                            AudioManager.STREAM_MUSIC
                        )

                    val volPer =
                        if (maxVolume > 0) {
                            ceil(
                                (
                                        mediaVolume.toDouble() /
                                                maxVolume.toDouble()
                                        ) * 100.0
                            )
                        } else {
                            0.0
                        }

                    volume_no?.text =
                        volPer.toString()
                }

                override fun onStartTrackingTouch(
                    seekBar: SeekBar?
                ) {
                    // Nothing to do
                }

                override fun onStopTrackingTouch(
                    seekBar: SeekBar?
                ) {
                    // Nothing to do
                }
            }
        )

        cross?.setOnClickListener {
            dismiss()
        }

        return builder.create()
    }
}