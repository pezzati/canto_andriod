package com.hmomeni.canto.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProviders
import com.hmomeni.canto.R
import com.hmomeni.canto.adapters.rcl.PaymentPacksRclAdapter
import com.hmomeni.canto.utils.ViewModelFactory
import com.hmomeni.canto.utils.app
import com.hmomeni.canto.utils.iomain
import com.hmomeni.canto.vms.PaymentViewModel
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_shop.*
import timber.log.Timber
import java.text.NumberFormat
import java.util.*

class ShopActivity : BaseActivity() {

    lateinit var viewModel: PaymentViewModel
    private var mAdapter: PaymentPacksRclAdapter? = null
    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this, ViewModelFactory(app()))[PaymentViewModel::class.java]

        mAdapter.takeIf { it == null }.let {
            mAdapter = PaymentPacksRclAdapter(viewModel.items).also {
                it.clickPublisher.subscribe {
                    val pack = viewModel.items[it]
                    startActivity(Intent(this@ShopActivity, PaymentActivity::class.java).putExtra("pack", pack))
                }
            }
        }
        setContentView(R.layout.activity_shop)

        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        recyclerView.adapter = mAdapter

        disposable = viewModel.getPaymentPacks()
                .iomain()
                .doOnSubscribe { progressBar.visibility = View.VISIBLE }
                .doAfterTerminate { progressBar.visibility = View.GONE }
                .subscribe({
                    mAdapter?.notifyDataSetChanged()
                }, {
                    Timber.e(it)
                })
    }

    override fun onResume() {
        super.onResume()
        viewModel.userSession.user?.let {
            currentBalance.text = NumberFormat.getInstance(Locale.ENGLISH).format(it.coins)
            daysRemaining.text = it.premiumDays.toString()
        }
    }

    override fun onStop() {
        disposable?.dispose()
        super.onStop()
    }
}
