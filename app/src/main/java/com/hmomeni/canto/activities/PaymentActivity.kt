package com.hmomeni.canto.activities

import android.content.Intent
import android.os.Bundle
import com.hmomeni.canto.BuildConfig
import com.hmomeni.canto.R
import com.hmomeni.canto.utils.billing.IabHelper
import com.hmomeni.canto.utils.billing.Purchase

class PaymentActivity : BaseActivity() {
    init {
        System.loadLibrary("Store")
    }

    companion object {
        const val PURCHASE_CODE = 4783
        const val TEST_SKU = "ir.canto_app.test"
    }

    private external fun getPaymentKey(market: String): String

    private lateinit var iabHelper: IabHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        iabHelper = IabHelper(this, getPaymentKey(BuildConfig.FLAVOR))

        iabHelper.startSetup {
            if (it.isFailure) {

            } else {
                queryInventory()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        iabHelper.handleActivityResult(requestCode, resultCode, data)
    }

    private fun queryInventory() {
        iabHelper.queryInventoryAsync { result, inv ->
            if (result.isSuccess) {
                if (inv.hasPurchase(TEST_SKU)) {
                    verifyPurchase(inv.getPurchase(TEST_SKU))
                } else {
                    iabHelper.launchPurchaseFlow(this, TEST_SKU, PURCHASE_CODE, { result, purchase ->
                        if (result.isSuccess) {
                            verifyPurchase(purchase)
                        } else {

                        }
                    }, "TEST_PAYLOAD")
                }
            } else {

            }
        }
    }

    private fun verifyPurchase(purchase: Purchase) {
        iabHelper.consumeAsync(purchase) { _, result ->
            if (result.isSuccess) {

            } else {

            }
        }
    }


}
