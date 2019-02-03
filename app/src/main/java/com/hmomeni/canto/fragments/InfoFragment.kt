package com.hmomeni.canto.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.hmomeni.canto.BuildConfig
import com.hmomeni.canto.R
import com.hmomeni.canto.utils.CantoDialog
import com.hmomeni.canto.utils.PaymentDialog
import kotlinx.android.synthetic.main.fragment_info.*
import java.util.*

class InfoFragment : BaseFragment(), View.OnClickListener {
    override fun onClick(v: View) {
        when (v.id) {
            R.id.backBtn -> findNavController().popBackStack()
            R.id.telegramChBtn -> openLink("http://t.me/cantoapp")
            R.id.telegramSupportBtn -> openLink("http://t.me/cantoapp")
            R.id.instagramBtn -> openLink("http://instagram.com/canto_app")
            R.id.webSiteBtn -> openLink("http://canto-app.ir")
            R.id.termsBtn -> CantoDialog(context!!, getString(R.string.terms_and_conditions), getString(R.string.terms), showNegativeButton = false).show()
            R.id.requestSongBtn -> PaymentDialog(
                    context!!,
                    getString(R.string.request_song),
                    getString(R.string.request_song_desc),
                    showTextInput = true,
                    showPositiveButton = true,
                    showNegativeButton = true,
                    positiveButtonText = getString(R.string.send),
                    textInputHint = getString(R.string.name_of_song_singer),
                    positiveListener = { _, _ ->

                    }
            ).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        telegramChBtn.setOnClickListener(this)
        telegramSupportBtn.setOnClickListener(this)
        webSiteBtn.setOnClickListener(this)
        instagramBtn.setOnClickListener(this)
        termsBtn.setOnClickListener(this)
        requestSongBtn.setOnClickListener(this)
        inviteBtn.setOnClickListener(this)
        backBtn.setOnClickListener(this)

        version.text = "Ver: %s".format(Locale.ENGLISH, BuildConfig.VERSION_NAME)
    }

    private fun openLink(link: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
    }
}
