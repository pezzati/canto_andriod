package com.hmomeni.canto.adapters.rcl

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
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
        private val activeColor = Color.parseColor("#FFFFFFFF")
        private val inActiveColor = Color.parseColor("#88FFFFFF")
        fun bind(midiItem: MidiItem) {
//            Timber.d("active=%b, verse=%s", midiItem.active, midiItem.text)
            itemView.textView.text = midiItem.text.trim('\n')
            if (midiItem.active) {
                itemView.textView.setTextColor(activeColor)
                itemView.textView.setTextColor(activeColor)
                itemView.textView.setTypeface(itemView.textView.typeface, Typeface.BOLD)
            } else {
                itemView.textView.setTextColor(inActiveColor)
                itemView.textView.setTypeface(itemView.textView.typeface, Typeface.NORMAL)
            }
        }
    }
}