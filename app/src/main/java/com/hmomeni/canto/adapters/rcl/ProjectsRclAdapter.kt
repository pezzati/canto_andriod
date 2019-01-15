package com.hmomeni.canto.adapters.rcl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hmomeni.canto.R
import com.hmomeni.canto.activities.RATIO_FULLSCREEN
import com.hmomeni.canto.activities.RATIO_SQUARE
import com.hmomeni.canto.entities.CompleteProject
import com.hmomeni.canto.utils.GlideApp
import com.hmomeni.canto.utils.dpToPx
import com.hmomeni.canto.utils.rounded
import io.reactivex.processors.PublishProcessor
import kotlinx.android.synthetic.main.rcl_item_project_portrait.view.*
import java.io.File

class ProjectsRclAdapter(private val projects: List<CompleteProject>) : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {

    val clickPublisher: PublishProcessor<Int> = PublishProcessor.create()

    override fun getItemViewType(position: Int): Int {
        return projects[position].ratio
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
        return when (viewType) {
            RATIO_SQUARE -> SquareHolder(LayoutInflater.from(parent.context).inflate(R.layout.rcl_item_project_square, parent, false), clickPublisher)
            RATIO_FULLSCREEN -> PortraitHolder(LayoutInflater.from(parent.context).inflate(R.layout.rcl_item_project_portrait, parent, false), clickPublisher)
            else -> throw RuntimeException("Invalid ViewType")
        }
    }

    override fun getItemCount() = projects.size

    override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SquareHolder -> holder.bind(projects[position])
            is PortraitHolder -> holder.bind(projects[position])
        }
    }

    class SquareHolder(itemView: View, clickPublisher: PublishProcessor<Int>) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {
                clickPublisher.onNext(adapterPosition)
            }
        }

        fun bind(project: CompleteProject) {
            GlideApp.with(itemView.preview)
                    .load(File(project.filePath))
                    .rounded(dpToPx(15))
                    .into(itemView.preview)

            itemView.artistName.text = project.post.artist?.name
            itemView.trackName.text = project.post.name
        }
    }

    class PortraitHolder(itemView: View, clickPublisher: PublishProcessor<Int>) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {
                clickPublisher.onNext(adapterPosition)
            }
        }

        fun bind(project: CompleteProject) {
            GlideApp.with(itemView.preview)
                    .load(File(project.filePath))
                    .rounded(dpToPx(15))
                    .into(itemView.preview)
            project.post.coverPhoto?.link?.let {
                GlideApp.with(itemView.postImageView)
                        .load(it)
                        .rounded(dpToPx(15))
                        .into(itemView.postImageView)
            }


            itemView.artistName.text = project.post.artist?.name
            itemView.trackName.text = project.post.name
        }
    }
}