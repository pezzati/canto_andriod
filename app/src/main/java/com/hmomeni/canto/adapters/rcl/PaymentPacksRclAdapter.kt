package com.hmomeni.canto.adapters.rcl

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hmomeni.canto.R
import com.hmomeni.canto.entities.PaymentPackage
import com.hmomeni.canto.utils.GlideApp
import io.reactivex.processors.PublishProcessor
import kotlinx.android.synthetic.main.rcl_item_payment_pack.view.*

class PaymentPacksRclAdapter(private val items: List<PaymentPackage>) : RecyclerView.Adapter<PaymentPacksRclAdapter.PaymentPackHolder>() {

    val clickPublisher: PublishProcessor<Int> = PublishProcessor.create()

    override fun onCreateViewHolder(parent: ViewGroup, itemType: Int): PaymentPackHolder {
        return PaymentPackHolder(LayoutInflater.from(parent.context).inflate(R.layout.rcl_item_payment_pack, parent, false), clickPublisher)
    }

    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: PaymentPackHolder, position: Int) {
        holder.bind(items[position])
    }

    class PaymentPackHolder(itemView: View, clickPublisher: PublishProcessor<Int>) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {
                clickPublisher.onNext(adapterPosition)
            }
        }

        fun bind(pack: PaymentPackage) {
            GlideApp.with(itemView.imageView)
                    .load(pack.icon)
                    .into(itemView.imageView)
        }
    }
}