package com.hmomeni.canto.utils

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.annotation.DrawableRes
import com.hmomeni.canto.R
import kotlinx.android.synthetic.main.dialog_payment.*

class PaymentDialog(
        context: Context,
        var title: String? = null,
        var content: String? = null,
        var imageUrl: String? = null,
        @DrawableRes
        var imageResId: Int = -1,
        var showPositiveButton: Boolean = true,
        var showNegativeButton: Boolean = false,
        var positiveButtonText: String? = null,
        var negativeButtonText: String? = null,
        var autoDismiss: Boolean = true,
        var positiveListener: ((PaymentDialog, String?) -> Unit)? = null,
        var negativeListener: ((PaymentDialog) -> Unit)? = null,
        var overlayText: String? = null,
        var showTextInput: Boolean = false,
        var textInputHint: String? = null
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_payment)

        window?.setLayout(context.resources.getDimensionPixelSize(R.dimen.dialog_width), WindowManager.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawableResource(android.R.color.transparent)


        title?.let {
            dialogTitle.text = it
        }
        content?.let {
            dialogContent.text = it
        }

        imageUrl?.let {
            GlideApp.with(context)
                    .load(it)
                    .rounded(dpToPx(15))
                    .into(dialogImage)
            dialogImage.apply {
                layoutParams = layoutParams.apply {
                    setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
                }
            }

        }
        if (imageResId > 0) {
            dialogImage.setImageResource(imageResId)
        }

        overlayText?.let {
            overlay.visibility = View.VISIBLE
            overlay.text = overlayText
        }

        if (!showPositiveButton) {
            positiveButton.visibility = View.GONE
        }

        if (showNegativeButton) {
            negativeButton.visibility = View.VISIBLE
        }

        positiveButtonText?.let {
            positiveButton.text = it
        }
        negativeButtonText?.let {
            negativeButton.text = it
        }
        positiveButton.setOnClickListener {
            positiveListener?.invoke(this, textInput.text.toString())
            if (autoDismiss) {
                dismiss()
            }
        }

        negativeButton.setOnClickListener {
            negativeListener?.invoke(this)
            if (autoDismiss) {
                dismiss()
            }
        }

        if (showTextInput) {
            textInput.visibility = View.VISIBLE
            textInputHint?.let {
                textInput.hint = textInputHint
            }
        } else {
            textInput.visibility = View.GONE
        }
    }
}