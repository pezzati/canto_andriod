package com.hmomeni.canto.adapters.rcl

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.hmomeni.canto.R
import com.hmomeni.canto.activities.RATIO_FULLSCREEN
import com.hmomeni.canto.activities.RATIO_SQUARE
import com.hmomeni.canto.entities.CompleteProject
import com.hmomeni.canto.utils.dpToPx
import com.hmomeni.canto.utils.rounded
import kotlinx.android.synthetic.main.rcl_item_project_portrait.view.*
import java.io.File

class ProjectsRclAdapter(private val projects: List<CompleteProject>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return projects[position].ratio
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            RATIO_SQUARE -> SquareHolder(LayoutInflater.from(parent.context).inflate(R.layout.rcl_item_project_square, parent, false))
            RATIO_FULLSCREEN -> PortraitHolder(LayoutInflater.from(parent.context).inflate(R.layout.rcl_item_project_portrait, parent, false))
            else -> throw RuntimeException("Invalid ViewType")
        }
    }

    override fun getItemCount() = projects.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SquareHolder -> holder.bind(projects[position])
            is PortraitHolder -> holder.bind(projects[position])
        }
    }

    class SquareHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(project: CompleteProject) {
            Glide
                    .with(itemView.preview)
                    .load(File(project.filePath))
                    .rounded(dpToPx(15))
                    .into(itemView.preview)

            itemView.artistName.text = project.post.artist.name
            itemView.trackName.text = project.post.name
        }
    }

    class PortraitHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(project: CompleteProject) {
            Glide
                    .with(itemView.preview)
                    .load(File(project.filePath))
                    .rounded(dpToPx(15))
                    .into(itemView.preview)

            Glide
                    .with(itemView.postImageView)
                    .load(project.post.coverPhoto)
                    .rounded(dpToPx(15))
                    .into(itemView.postImageView)

            itemView.artistName.text = project.post.artist.name
            itemView.trackName.text = project.post.name
        }
    }
}