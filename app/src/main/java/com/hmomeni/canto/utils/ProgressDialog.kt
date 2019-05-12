package com.hmomeni.canto.utils

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import com.hmomeni.canto.R
import kotlinx.android.synthetic.main.dialog_progress.*

class ProgressDialog(
        context: Context,
        var title: String? = null

) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_progress)

        window?.setLayout(context.resources.getDimensionPixelSize(R.dimen.dialog_width), WindowManager.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawableResource(android.R.color.transparent)


        title?.let {
            dialogTitle.text = it
        }
    }
}