package com.hmomeni.canto.vms

import androidx.lifecycle.ViewModel
import com.hmomeni.canto.api.Api
import com.hmomeni.canto.di.DIComponent
import com.hmomeni.canto.entities.PaymentPackage
import com.hmomeni.canto.entities.UserInventory
import com.hmomeni.canto.utils.makeMap
import io.reactivex.Completable
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject

class PaymentViewModel : ViewModel(), DIComponent.Injectable {
    override fun inject(diComponent: DIComponent) {
        diComponent.inject(this)
    }

    @Inject
    lateinit var api: Api
    @Inject
    lateinit var userInventory: UserInventory

    val items: MutableList<PaymentPackage> = mutableListOf()
    lateinit var pack: PaymentPackage

    fun getPaymentPacks(): Single<List<PaymentPackage>> = if (items.isEmpty()) api.getPaymentPacks()
            .map {
                items.clear()
                items.addAll(it.data)
                return@map items
            } else Single.just(items)

    fun createInvoice(): Completable {
        val map = makeMap().add("serial_number", pack.sku)

        return api.createInvoice(map.body())
                .doOnSuccess {
                    val invoiceId = it["serial_number"].asString
                    pack.invoiceId = invoiceId
                }
                .ignoreElement()
    }

    fun verifyPayment(invoiceId: String, paymentToken: String): Completable {
        pack.paymentToken = paymentToken
        pack.invoiceId = invoiceId
        val map = makeMap()
                .add("serial_number", pack.invoiceId!!)
                .add("ref_id", paymentToken)

        return api.verifyPayment(map.body())
                .doOnSuccess {
                    val coin = it["coins"].asInt
                    Timber.d("New Coin=%d", coin)
                }
                .ignoreElement()
    }
}