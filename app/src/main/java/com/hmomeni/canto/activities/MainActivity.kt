package com.hmomeni.canto.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.crashlytics.android.Crashlytics
import com.hmomeni.canto.App
import com.hmomeni.canto.R
import com.hmomeni.canto.entities.Post
import com.hmomeni.canto.fragments.ListFragment
import com.hmomeni.canto.services.FFMpegService
import com.hmomeni.canto.utils.*
import com.hmomeni.canto.utils.navigation.*
import com.hmomeni.canto.vms.MainViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.HttpException
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class MainActivity : BaseActivity() {

    @Inject
    lateinit var navEvents: PublishProcessor<NavEvent>
    @Inject
    lateinit var userSession: UserSession

    private lateinit var viewModel: MainViewModel

    private lateinit var navController: NavController

    private val compositeDisposable = CompositeDisposable()

    private var navDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, ViewModelFactory(app()))[MainViewModel::class.java]

        (application as App).di.inject(this)

        startService(Intent(this, FFMpegService::class.java))

        if (!userSession.isUser()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        if (intent.hasExtra("new_user") && intent.getBooleanExtra("new_user", false)) {
            Intent(this, EditUserActivity::class.java)
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
                            openPost(it.post)
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
        handshake()
    }

    private fun handshake() {
        viewModel.handshake(app())
                .iomain().subscribe({ p ->
                    when (p.first) {
                        2 -> {
                            PaymentDialog(this,
                                    getString(R.string.update_required),
                                    getString(R.string.update_suggest_rationale),
                                    imageResId = R.drawable.update,
                                    showPositiveButton = true,
                                    showNegativeButton = true,
                                    positiveButtonText = getString(R.string.update),
                                    negativeButtonText = getString(R.string.ask_later),
                                    positiveListener = {
                                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(p.second)))
                                    }).show()
                        }
                        1 -> PaymentDialog(this,
                                getString(R.string.force_update),
                                getString(R.string.update_force_rationale),
                                imageResId = R.drawable.force_update,
                                showPositiveButton = true,
                                showNegativeButton = false,
                                autoDismiss = false,
                                positiveButtonText = getString(R.string.update),
                                positiveListener = {
                                    if (!p.second.isNullOrEmpty()) {
                                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(p.second)))
                                    }
                                }).apply { setCanceledOnTouchOutside(false) }.show()
                    }
                }, {
                    PaymentDialog(this,
                            getString(R.string.no_network),
                            getString(R.string.please_check_internet),
                            imageResId = R.drawable.no_internet_conation,
                            showPositiveButton = false,
                            showNegativeButton = true,
                            negativeButtonText = getString(R.string.ok),
                            negativeListener = {
                                handshake()
                            }).show()
                }).addTo(compositeDisposable)

        viewModel.getUser().iomain().subscribe({}, {}).addTo(compositeDisposable)
    }

    private fun openPost(post: Post) {
        val dialog = ProgressDialog(this)
        viewModel
                .sing(post)
                .iomain()
                .doOnSubscribe { dialog.show() }
                .doAfterTerminate { dialog.dismiss() }
                .subscribe({
                    if (navController.currentDestination!!.id != R.id.mainFragment) {
                        navController.popBackStack(R.id.mainFragment, false)
                    }
                    navController.navigate(R.id.action_mainFragment_to_recorderFragment, Bundle().apply { putInt("post_id", post.id) })
                }, {
                    if (it is HttpException) {
                        when (it.code()) {
                            HTTP_ERROR_PAYMENT_REQUIRED -> {
                                PaymentDialog(this, showNegativeButton = true, positiveListener = {
                                    startActivity(Intent(this, ShopActivity::class.java))
                                }).show()
                            }
                            HTTP_ERROR_NOT_PURCHASED -> {
                                PaymentDialog(
                                        this,
                                        title = getString(R.string.purchase_song),
                                        content = getString(R.string.are_you_sure_to_but_x_tries, post.count, post.name),
                                        imageUrl = post.coverPhoto?.link,
                                        showNegativeButton = true,
                                        positiveButtonText = getString(R.string.yes_buy),
                                        positiveListener = {
                                            purchaseSong(post)
                                        },
                                        overlayText = "X%d".format(Locale.ENGLISH, post.count)
                                ).show()
                            }
                        }
                    }
                    Timber.e(it)
                }).addTo(compositeDisposable)
    }

    private fun purchaseSong(post: Post) {
        val dialog = ProgressDialog(this)
        viewModel.purchaseSong(post)
                .iomain()
                .doOnSubscribe { dialog.show() }
                .doAfterTerminate { dialog.dismiss() }
                .subscribe({
                    viewModel.navEvents.onNext(PostNavEvent(post))
                }, {
                    if (it is HttpException && it.code() == HTTP_ERROR_PAYMENT_REQUIRED) {
                        PaymentDialog(this, showNegativeButton = true, positiveListener = {
                            startActivity(Intent(this, ShopActivity::class.java))
                        }).show()
                    } else {
                        Toast.makeText(this, R.string.purchase_song_failed, Toast.LENGTH_SHORT).show()
                        Crashlytics.logException(it)
                    }
                }).addTo(compositeDisposable)
    }

    override fun onDestroy() {
        navDisposable?.dispose()
        compositeDisposable.clear()
        super.onDestroy()
    }


}
