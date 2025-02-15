package com.hmomeni.canto.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.crashlytics.android.Crashlytics
import com.google.firebase.analytics.FirebaseAnalytics
import com.hmomeni.canto.App
import com.hmomeni.canto.R
import com.hmomeni.canto.entities.Post
import com.hmomeni.canto.entities.UserAction
import com.hmomeni.canto.fragments.MainFragmentDirections
import com.hmomeni.canto.services.FFMpegService
import com.hmomeni.canto.utils.*
import com.hmomeni.canto.utils.navigation.NavEvent
import com.hmomeni.canto.utils.navigation.PostNavEvent
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
import java.util.concurrent.TimeUnit
import javax.inject.Inject

val BOTTOM_NAV_FRAGMENTS = arrayOf(R.id.mainFragment, R.id.profileFragment, R.id.searchFragment, R.id.listFragment)

class MainActivity : BaseActivity() {

    @Inject
    lateinit var navEvents: PublishProcessor<NavEvent>

    private lateinit var viewModel: MainViewModel

    private lateinit var navController: NavController

    private val compositeDisposable = CompositeDisposable()

    private var navDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, ViewModelFactory(app()))[MainViewModel::class.java]

        (application as App).di.inject(this)

        startService(Intent(this, FFMpegService::class.java))

        scheduleUserActionSync()

        addUserAction(UserAction("App entered foreground"))

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
                        is PostNavEvent -> {
                            openPost(it.post)
                        }
                    }
                }
        bottomNav.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id !in BOTTOM_NAV_FRAGMENTS) {
                bottomNav.visibility = View.GONE
            } else {
                bottomNav.visibility = View.VISIBLE
            }
        }
        handshake()

        if (intent.hasExtra("new_user") && intent.getBooleanExtra("new_user", false)) {
            navController.navigate(MainFragmentDirections.actionMainFragmentToEditUserFragment())
        }
    }

    private fun handshake() {
        viewModel.handshake(app())
                .iomain().subscribe({ p ->
                    when (p.first) {
                        HANDSHAKE_SUGGEST_UPDATE -> {
                            PaymentDialog(this,
                                    getString(R.string.update_required),
                                    getString(R.string.update_suggest_rationale),
                                    imageResId = R.drawable.update,
                                    showPositiveButton = true,
                                    showNegativeButton = true,
                                    positiveButtonText = getString(R.string.update),
                                    negativeButtonText = getString(R.string.ask_later),
                                    positiveListener = { _, _ ->
                                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(p.second)))
                                    }).show()
                        }
                        HANDSHAKE_FORCE_UPDATE -> PaymentDialog(this,
                                getString(R.string.force_update),
                                getString(R.string.update_force_rationale),
                                imageResId = R.drawable.force_update,
                                showPositiveButton = true,
                                showNegativeButton = false,
                                autoDismiss = false,
                                positiveButtonText = getString(R.string.update),
                                positiveListener = { _, _ ->
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

        viewModel.getUser()
                .iomain()
                .subscribe({
                    FirebaseAnalytics.getInstance(this).setUserId(it.id.toString())
                    Crashlytics.setUserIdentifier(it.id.toString())
                }, {
                    Timber.e(it)
                }).addTo(compositeDisposable)
    }

    private fun openPost(post: Post) {
        val dialog = ProgressDialog(this)
        viewModel
                .sing(post)
                .iomain()
                .doOnSubscribe { dialog.show() }
                .doAfterTerminate { dialog.dismiss() }
                .logError()
                .subscribe({
                    addUserAction(UserAction("Karaoke Tapped", post.id.toString(), "Ok"))
                    if (navController.currentDestination!!.id != R.id.mainFragment) {
                        navController.popBackStack(R.id.mainFragment, false)
                    }
                    navController.navigate(R.id.action_mainFragment_to_recorderFragment, Bundle().apply { putInt("post_id", post.id) })
                }, {
                    if (it is HttpException) {
                        when (it.code()) {
                            HTTP_ERROR_PAYMENT_REQUIRED -> {
                                addUserAction(UserAction("Karaoke Tapped", post.id.toString(), "Payment required"))
                                PaymentDialog(this, showNegativeButton = true, positiveListener = { _, _ ->
                                    startActivity(Intent(this, ShopActivity::class.java))
                                    addUserAction(UserAction("Go to shop", post.id.toString(), "Tapped"))
                                }).apply {
                                    setOnDismissListener {
                                        addUserAction(UserAction("Go to shop", post.id.toString(), "canceled"))
                                    }
                                }.show()
                            }
                            HTTP_ERROR_NOT_PURCHASED -> {
                                addUserAction(UserAction("Karaoke Tapped", post.id.toString(), "Should buy"))
                                PaymentDialog(
                                        this,
                                        title = getString(R.string.purchase_song),
                                        content = getString(R.string.are_you_sure_to_but_x_tries, post.count, post.name),
                                        imageUrl = post.coverPhoto?.link,
                                        showNegativeButton = true,
                                        positiveButtonText = getString(R.string.yes_buy),
                                        positiveListener = { _, _ ->
                                            purchaseSong(post)
                                            addUserAction(UserAction("Buy song", post.id.toString(), "Tapped"))
                                        },
                                        overlayText = "X%d".format(Locale.ENGLISH, post.count)
                                ).apply {
                                    setOnDismissListener {
                                        addUserAction(UserAction("Buy song", post.id.toString(), "canceled"))
                                    }
                                }.show()
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
                        PaymentDialog(this, showNegativeButton = true, positiveListener = { _, _ ->
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
        addUserAction(UserAction("App entered background"))
        super.onDestroy()
    }

    private fun scheduleUserActionSync() {
        val userActionWorkBuilder = PeriodicWorkRequestBuilder<UserActionSyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build())
        WorkManager.getInstance().enqueue(userActionWorkBuilder.build())
    }

}
