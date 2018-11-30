package com.hmomeni.canto.adapters.rcl

import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hmomeni.canto.R
import com.hmomeni.canto.entities.MidiItem
import kotlinx.android.synthetic.main.rcl_item_midi.view.*

class LyricRclAdapter(private val midiItems: List<MidiItem>) : RecyclerView.Adapter<LyricRclAdapter.MidiHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MidiHolder {
        return MidiHolder(LayoutInflater.from(parent.context).inflate(R.layout.rcl_item_midi, parent, false))
    }

    override fun getItemCount() = midiItems.size

    override fun onBindViewHolder(holder: MidiHolder, position: Int) {
        holder.bind(midiItems[position])
    }

    class MidiHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(midiItem: MidiItem) {
            itemView.textView.text = midiItem.text.trim('\n')
            if (midiItem.active) {
                itemView.textView.setTextColor(Color.RED)
            } else {
                itemView.textView.setTextColor(Color.WHITE)
            }
        }
    }
}