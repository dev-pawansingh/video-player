package com.pawansingh.vp.models

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import com.pawansingh.vp.R

class BrightnessDialog : AppCompatDialogFragment() {

    private var bright_no: TextView? = null
    private var cross: ImageView? = null
    private var seekBar: SeekBar? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity()
        val builder = AlertDialog.Builder(activity)
        val inflater = activity.layoutInflater
        val view = inflater.inflate(
            R.layout.brt_dialog_item,
            null
        )

        builder.setView(view)

        cross = view.findViewById(R.id.brt_close)
        bright_no = view.findViewById(R.id.brt_number)
        seekBar = view.findViewById(R.id.brt_seekBar)

        val brightness = Settings.System.getInt(
            activity.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            0
        )

        bright_no?.text = brightness.toString()
        seekBar?.progress = brightness
        seekBar?.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {

                    val context = requireContext().applicationContext
                    val canWrite = Settings.System.canWrite(context)
                    if (canWrite) {
                        val sBrightness = progress * 255 / 255
                        bright_no?.text = sBrightness.toString()
                        Settings.System.putInt(
                            context.contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS_MODE,
                            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                        )
                        Settings.System.putInt(
                            context.contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS,
                            sBrightness
                        )
                    } else {
                        Toast.makeText(
                            context,
                            "Enable write settings for brightness control",
                            Toast.LENGTH_SHORT
                        ).show()

                        val intent =
                            Intent(
                                Settings.ACTION_MANAGE_WRITE_SETTINGS
                            )

                        intent.data =
                            Uri.parse(
                                "package:${context.packageName}"
                            )

                        startActivityForResult(
                            intent,
                            0
                        )
                    }
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
        cross?.setOnClickListener { dismiss() }
        return builder.create()
    }
}