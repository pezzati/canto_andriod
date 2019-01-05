package com.hmomeni.canto.entities

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

class PaymentPackage(
        @SerializedName("name")
        val name: String,
        @SerializedName("price")
        val price: Long,
        @SerializedName("icon")
        val icon: String,
        @SerializedName("serial_number")
        val sku: String,
        @SerializedName("package_type")
        val type: String,
        @SerializedName("invoice_id")
        var invoiceId: String? = null,
        @SerializedName("payment_token")
        var paymentToken: String? = null
) : Parcelable {
    constructor(source: Parcel) : this(
            source.readString(),
            source.readLong(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(name)
        writeLong(price)
        writeString(icon)
        writeString(sku)
        writeString(type)
        writeString(invoiceId)
        writeString(paymentToken)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PaymentPackage> = object : Parcelable.Creator<PaymentPackage> {
            override fun createFromParcel(source: Parcel): PaymentPackage = PaymentPackage(source)
            override fun newArray(size: Int): Array<PaymentPackage?> = arrayOfNulls(size)
        }
    }
}