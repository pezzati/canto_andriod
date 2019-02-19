package com.hmomeni.canto.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProviders
import com.google.firebase.analytics.FirebaseAnalytics
import com.hmomeni.canto.BuildConfig
import com.hmomeni.canto.R
import com.hmomeni.canto.utils.PaymentDialog
import com.hmomeni.canto.utils.ViewModelFactory
import com.hmomeni.canto.utils.app
import com.hmomeni.canto.utils.billing.IabHelper
import com.hmomeni.canto.utils.billing.Purchase
import com.hmomeni.canto.utils.iomain
import com.hmomeni.canto.vms.PaymentViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_payment.*
import timber.log.Timber

class PaymentActivity : BaseActivity() {
    init {
        System.loadLibrary("Store")
    }

    companion object {
        const val PURCHASE_CODE = 4783
    }

    private external fun getPaymentKey(market: String): String

    private lateinit var iabHelper: IabHelper

    private lateinit var viewModel: PaymentViewModel

    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!intent.hasExtra("pack")) {
            Toast.makeText(this, "Invalid Activity Call", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel = ViewModelProviders.of(this, ViewModelFactory(app()))[PaymentViewModel::class.java]

        setContentView(R.layout.activity_payment)

        viewModel.pack = intent.getParcelableExtra("pack")

        if (BuildConfig.payment == "zarinpal") {
            PaymentDialog(
                    this,
                    getString(R.string.payment),
                    getString(R.string.you_will_be_directed_to_payment_url),
                    imageResId = R.drawable.canto_logo,
                    showPositiveButton = true,
                    showNegativeButton = true,
                    positiveButtonText = getString(R.string.ok),
                    negativeButtonText = getString(R.string.cancel),
                    positiveListener = { _, _ -> createNewInvoice() }
            ).show()
        } else {
            iabHelper = IabHelper(this, getPaymentKey(BuildConfig.FLAVOR))

            iabHelper.startSetup {
                if (it.isFailure) {
                    Timber.d("IAB setup failed: %s", it.message)
                    showError()
                } else {
                    queryInventory()
                }
            }
        }
        backBtn.setOnClickListener { finish() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        iabHelper.handleActivityResult(requestCode, resultCode, data)
    }

    private fun queryInventory() {
        iabHelper.queryInventoryAsync { result, inv ->
            if (result.isSuccess) {
                if (inv.hasPurchase(viewModel.pack.sku)) {
                    verifyPurchase(inv.getPurchase(viewModel.pack.sku))
                } else {
                    createNewInvoice()
                }
            } else {
                Timber.d("Query inventory failed: %s", result.message)
                showError()
            }
        }
    }

    private fun createNewInvoice() {
        if (BuildConfig.payment == "zarinpal") {
            viewModel.createInvoiceZarinpal()
                    .iomain()
                    .doOnSuccess {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.trim('"'))))
                        finish()
                    }.ignoreElement()
        } else {
            viewModel.createInvoice()
                    .iomain()
                    .doOnComplete {
                        launchPurchase()
                    }
        }.subscribe({
            Timber.d("invoice successful")
        }, {
            if (it is ActivityNotFoundException) {
                Toast.makeText(this, R.string.please_install_browser, Toast.LENGTH_SHORT).show()
            }
            Timber.e(it)
            showError()
        }).addTo(compositeDisposable)
    }

    private fun launchPurchase() {
        iabHelper.launchPurchaseFlow(this, viewModel.pack.sku, PURCHASE_CODE, { result, purchase ->
            if (result.isSuccess) {
                verifyPurchase(purchase)
            } else {
                Timber.d("Purchase failed: %s", result.message)
                showError()
            }
        }, viewModel.pack.invoiceId)
    }

    private fun verifyPurchase(purchase: Purchase) {
        viewModel.verifyPayment(purchase.developerPayload, purchase.token)
                .iomain()
                .subscribe({
                    iabHelper.consumeAsync(purchase) { _, result ->
                        if (result.isSuccess) {
                            FirebaseAnalytics.getInstance(this)
                                    .logEvent(FirebaseAnalytics.Event.ECOMMERCE_PURCHASE, Bundle().apply {
                                        putString(FirebaseAnalytics.Param.ITEM_ID, purchase.sku)
                                    })
                            showSuccess()
                        } else {
                            Timber.e("Consuming purchase failed: %s", result.message)
                            showError()
                        }
                    }
                }, {
                    Timber.e(it)
                    showError()
                }).addTo(compositeDisposable)
    }

    private fun showError() {
        imageView.setImageResource(R.drawable.payment_failed)
        pageTitle.setText(R.string.payment_failed)
        resultDesc.setText(R.string.purchase_failed_desc)
        backBtn.visibility = View.VISIBLE
    }

    private fun showSuccess() {
        imageView.setImageResource(R.drawable.payment_success)
        pageTitle.setText(R.string.payment_successful)
        resultDesc.text = getString(R.string.you_purchased_pack_x, viewModel.pack.name)
        backBtn.visibility = View.VISIBLE
    }


}
