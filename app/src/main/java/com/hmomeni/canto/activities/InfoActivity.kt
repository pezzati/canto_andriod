package com.hmomeni.canto.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.hmomeni.canto.BuildConfig
import com.hmomeni.canto.R
import kotlinx.android.synthetic.main.activity_info.*
import java.util.*

class InfoActivity : AppCompatActivity(), View.OnClickListener {
    override fun onClick(v: View) {
        when (v.id) {
            R.id.telegramChBtn -> openLink("http://t.me/cantoapp")
            R.id.telegramSupportBtn -> openLink("http://t.me/cantoapp")
            R.id.instagramBtn -> openLink("http://instagram.com/canto_app")
            R.id.webSiteBtn -> openLink("http://canto-app.ir")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        telegramChBtn.setOnClickListener(this)
        telegramSupportBtn.setOnClickListener(this)
        webSiteBtn.setOnClickListener(this)
        instagramBtn.setOnClickListener(this)
        termsBtn.setOnClickListener(this)
        requestSongBtn.setOnClickListener(this)
        inviteBtn.setOnClickListener(this)

        version.text = "Ver: %s".format(Locale.ENGLISH, BuildConfig.VERSION_NAME)
    }

    private fun openLink(link: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
    }
}
