package com.hmomeni.canto.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.hmomeni.canto.R
import com.hmomeni.canto.adapters.rcl.PaymentPacksRclAdapter
import com.hmomeni.canto.entities.UserAction
import com.hmomeni.canto.utils.*
import com.hmomeni.canto.vms.PaymentViewModel
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_shop.*
import timber.log.Timber
import java.text.NumberFormat
import java.util.*

class ShopActivity : BaseActivity() {

    lateinit var viewModel: PaymentViewModel
    private var mAdapter: PaymentPacksRclAdapter? = null
    private val compositeDisposable = CompositeDisposable()
    private var giftCodeValidate = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this, ViewModelFactory(app()))[PaymentViewModel::class.java]

        mAdapter.takeIf { it == null }.let {
            mAdapter = PaymentPacksRclAdapter(viewModel.items).also {
                it.clickPublisher.subscribe {
                    val pack = viewModel.items[it]
                    addUserAction(UserAction("Package tapped", pack.sku))
                    startActivity(Intent(this@ShopActivity, PaymentActivity::class.java).putExtra("pack", pack))
                }
            }
        }
        setContentView(R.layout.activity_shop)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = mAdapter

        viewModel.getPaymentPacks()
                .iomain()
                .doOnSubscribe { progressBar.visibility = View.VISIBLE }
                .doAfterTerminate { progressBar.visibility = View.GONE }
                .subscribe({
                    mAdapter?.notifyDataSetChanged()
                }, {
                    Timber.e(it)
                }).addTo(compositeDisposable)

        giftCodeBtn.setOnClickListener {
            val progressDialog: ProgressDialog = ProgressDialog(this).apply {
                setCanceledOnTouchOutside(false)
            }
            if (giftCodeValidate) {
                viewModel.applyGiftCode(giftCodeInpt.text.toString())
                        .iomain()
                        .doOnSubscribe { progressDialog.show() }
                        .doAfterTerminate { progressDialog.dismiss() }
                        .subscribe({
                            PaymentDialog(this, getString(R.string.congrats), getString(R.string.gift_code_is_applied), imageResId = R.drawable.gift_code, positiveButtonText = getString(R.string.back)).show()
                            giftCodeValidate = false
                            giftResultWrapper.gone()
                        }, {
                            Timber.e(it)
                        }).addTo(compositeDisposable)
            } else {
                viewModel.validateGiftCode(giftCodeInpt.text.toString())
                        .iomain()
                        .doOnSubscribe { progressDialog.show() }
                        .doAfterTerminate {
                            giftResultWrapper.visible()
                            progressDialog.dismiss()
                        }
                        .subscribe({
                            giftCodeValidate = true
                            giftCodeBtn.setText(R.string.apply)
                            giftValidateResult.setText(R.string.gift_code_is_valid)
                            giftValidateResultImg.setImageResource(R.drawable.ic_success)
                        }, {
                            Timber.e(it)
                            giftCodeValidate = false
                            giftValidateResult.setText(R.string.gift_code_is_invalid)
                            giftValidateResultImg.setImageResource(R.drawable.ic_error)
                        }).addTo(compositeDisposable)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.userSession.user?.let {
            currentBalance.text = NumberFormat.getInstance(Locale.ENGLISH).format(it.coins)
            daysRemaining.text = it.premiumDays.toString()
        }
    }

    override fun onStart() {
        super.onStart()
        giftCodeInpt.textChanges()
                .skipInitialValue()
                .subscribe {
                    giftCodeBtn.setText(R.string.validate)
                    giftCodeValidate = false
                    giftResultWrapper.gone()
                }.addTo(compositeDisposable)
    }

    override fun onStop() {
        compositeDisposable.clear()
        super.onStop()
    }
}
