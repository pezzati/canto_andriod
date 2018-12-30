package com.hmomeni.canto.activities

import android.os.Bundle
import com.hmomeni.canto.BuildConfig
import com.hmomeni.canto.R
import com.hmomeni.canto.utils.billing.IabHelper

class PaymentActivity : BaseActivity() {
    init {
        System.loadLibrary("Store")
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

            }
        }
    }
}
