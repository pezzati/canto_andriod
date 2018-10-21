package com.hmomeni.canto.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.hmomeni.canto.App
import com.hmomeni.canto.R
import com.pixplicity.easyprefs.library.Prefs
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var app: App

    lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Prefs.getString("token", "").isEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        (application as App).di.inject(this)

        setContentView(R.layout.activity_main)
        navController = findNavController(R.id.mainNav)

    }


}
