package com.hmomeni.canto.utils

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import com.hmomeni.canto.R
import kotlinx.android.synthetic.main.dialog_alert.*

class CantoDialog(
        context: Context,
        var title: String,
        var content: String,
        var showNegativeButton: Boolean = false,
        var positiveButtonText: String? = null,
        var negativeButtonText: String? = null,
        var autoDismiss: Boolean = true,
        var positiveListener: ((CantoDialog) -> Unit)? = null,
        var negativeListener: ((CantoDialog) -> Unit)? = null
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_alert)

        window.setLayout(context.resources.getDimensionPixelSize(R.dimen.dialog_width), WindowManager.LayoutParams.WRAP_CONTENT)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        dialogTitle.text = title
        dialogContent.text = content

        if (showNegativeButton) {
            vr.visibility = View.VISIBLE
            negativeButton.visibility = View.VISIBLE
        }

        positiveButtonText?.let {
            positiveButton.text = it
        }
        negativeButtonText?.let {
            negativeButton.text = it
        }

        positiveListener?.let {
            positiveButton.setOnClickListener {
                positiveListener?.invoke(this)
                if (autoDismiss) {
                    dismiss()
                }
            }
        } ?: run {
            positiveButton.setOnClickListener {
                if (autoDismiss) {
                    dismiss()
                }
            }
        }
        negativeListener?.let {
            negativeButton.setOnClickListener {
                negativeListener?.invoke(this)
                if (autoDismiss) {
                    dismiss()
                }
            }
        }
    }
}