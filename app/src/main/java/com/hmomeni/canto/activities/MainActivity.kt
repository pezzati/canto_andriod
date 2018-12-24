package com.hmomeni.canto.activities

import android.content.Intent
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.hmomeni.canto.App
import com.hmomeni.canto.R
import com.hmomeni.canto.fragments.ListFragment
import com.hmomeni.canto.utils.UserSession
import com.hmomeni.canto.utils.navigation.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.processors.PublishProcessor
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

class MainActivity : BaseActivity() {

    @Inject
    lateinit var navEvents: PublishProcessor<NavEvent>
    @Inject
    lateinit var userSession: UserSession

    private lateinit var navController: NavController

    private var navDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (application as App).di.inject(this)

        if (!userSession.isUser()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

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
                            navController.navigate(R.id.action_mainFragment_to_listFragment, ListFragment.getBundle(it.type, it.objectId, it.title, it.urlPath))
                        }
                        is SearchEvent -> {
                            if (navController.currentDestination!!.id != R.id.mainFragment) {
                                navController.popBackStack(R.id.mainFragment, false)
                            }
                            navController.navigate(R.id.action_mainFragment_to_searchFragment)
                        }
                        is PostNavEvent -> {
                            if (navController.currentDestination!!.id != R.id.mainFragment) {
                                navController.popBackStack(R.id.mainFragment, false)
                            }
                            navController.navigate(R.id.action_mainFragment_to_recorderFragment, Bundle().apply { putInt("post_id", it.post.id) })
                        }
                        is ProfileEvent -> {
                            if (navController.currentDestination!!.id != R.id.mainFragment) {
                                navController.popBackStack(R.id.mainFragment, false)
                            }
                            navController.navigate(R.id.action_mainFragment_to_profileFragment)
                        }
                        is ProjectEvent -> {
                            navController.navigate(R.id.action_profileFragment_to_videoPlayActivity, VideoPlayActivityArgs.Builder(it.projectId.toInt()).build().toBundle())
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
                R.id.navHome -> navController.popBackStack(R.id.mainFragment, false)
                R.id.navSearch -> navEvents.onNext(SearchEvent())
                R.id.navProfile -> navEvents.onNext(ProfileEvent())
            }
            return@setOnNavigationItemSelectedListener true
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.mainFragment -> {
                    userNavFired = true
                    bottomNav.selectedItemId = R.id.navHome
                }
                R.id.searchFragment -> {
                    userNavFired = true
                    bottomNav.selectedItemId = R.id.navSearch
                }
                R.id.profileFragment -> {
                    userNavFired = true
                    bottomNav.selectedItemId = R.id.navProfile
                }
            }
        }

    }

    override fun onDestroy() {
        navDisposable?.dispose()
        super.onDestroy()
    }


}
