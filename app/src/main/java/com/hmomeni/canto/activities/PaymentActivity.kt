package com.hmomeni.canto.activities

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.hmomeni.canto.BuildConfig
import com.hmomeni.canto.R
import com.hmomeni.canto.utils.ViewModelFactory
import com.hmomeni.canto.utils.app
import com.hmomeni.canto.utils.billing.IabHelper
import com.hmomeni.canto.utils.billing.Purchase
import com.hmomeni.canto.utils.iomain
import com.hmomeni.canto.vms.PaymentViewModel
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

        iabHelper = IabHelper(this, getPaymentKey(BuildConfig.FLAVOR))

        iabHelper.startSetup {
            if (it.isFailure) {
                Timber.d("IAB setup failed: %s", it.message)
                showError()
            } else {
                queryInventory()
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
        viewModel.createInvoice()
                .iomain()
                .subscribe({
                    iabHelper.launchPurchaseFlow(this, viewModel.pack.sku, PURCHASE_CODE, { result, purchase ->
                        if (result.isSuccess) {
                            verifyPurchase(purchase)
                        } else {
                            Timber.d("Purchase failed: %s", result.message)
                            showError()
                        }
                    }, viewModel.pack.invoiceId)

                }, {
                    Timber.e(it)
                    showError()
                })
    }

    private fun verifyPurchase(purchase: Purchase) {
        viewModel.verifyPayment(purchase.developerPayload, purchase.token)
                .iomain()
                .subscribe({
                    iabHelper.consumeAsync(purchase) { _, result ->
                        if (result.isSuccess) {
                            showSuccess()
                        } else {
                            Timber.e("Consuming purchase failed: %s", result.message)
                            showError()
                        }
                    }
                }, {
                    Timber.e(it)
                    showError()
                })
    }

    private fun showError() {
        imageView.setImageResource(R.drawable.payment_success)
        pageTitle.setText(R.string.payment_successful)
        resultDesc.text = getString(R.string.you_purchased_pack_x, viewModel.pack.name)
        backBtn.visibility = View.VISIBLE
    }

    private fun showSuccess() {
        imageView.setImageResource(R.drawable.payment_failed)
        pageTitle.setText(R.string.payment_failed)
        backBtn.visibility = View.VISIBLE
    }


}
