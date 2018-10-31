package com.hmomeni.canto.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.hmomeni.canto.App
import com.hmomeni.canto.R
import com.hmomeni.canto.fragments.ListFragment
import com.hmomeni.canto.utils.navigation.BackEvent
import com.hmomeni.canto.utils.navigation.ListNavEvent
import com.hmomeni.canto.utils.navigation.NavEvent
import com.hmomeni.canto.utils.navigation.SearchEvent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.processors.PublishProcessor
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var app: App
    @Inject
    lateinit var navEvents: PublishProcessor<NavEvent>

    private lateinit var navController: NavController

    private var navDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*if (Prefs.getString("token", "").isEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }*/

        (application as App).di.inject(this)

        setContentView(R.layout.activity_main)
        navController = findNavController(R.id.mainNav)
        navDisposable = navEvents
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    when (it) {
                        is BackEvent -> {
                            navController.navigateUp()
                        }
                        is ListNavEvent -> {
                            navController.navigate(R.id.action_mainFragment_to_listFragment, ListFragment.getBundle(it.type, it.objectId, it.title))
                        }
                        is SearchEvent -> {
                            if (navController.currentDestination!!.id != R.id.mainFragment) {
                                navController.popBackStack(R.id.mainFragment, false)
                            }
                            navController.navigate(R.id.action_mainFragment_to_searchFragment)
                        }
                    }
                }
        var userNavFired = false
        bottomNav.setOnNavigationItemSelectedListener {
            if (userNavFired) {
                userNavFired = false
                return@setOnNavigationItemSelectedListener true
            }
            when (it.itemId) {
                R.id.navHome -> {
                    navController.popBackStack(R.id.mainFragment, false)
                }
                R.id.navSearch -> {
                    navEvents.onNext(SearchEvent())
                }
                else -> {
                }
            }
            return@setOnNavigationItemSelectedListener true
        }

        navController.addOnNavigatedListener { _, destination ->
            when (destination.id) {
                R.id.mainFragment -> {
                    userNavFired = true
                    bottomNav.selectedItemId = R.id.navHome
                }
                R.id.searchFragment -> {
                    userNavFired = true
                    bottomNav.selectedItemId = R.id.navSearch
                }
            }
        }

    }

    override fun onDestroy() {
        navDisposable?.dispose()
        super.onDestroy()
    }


}
