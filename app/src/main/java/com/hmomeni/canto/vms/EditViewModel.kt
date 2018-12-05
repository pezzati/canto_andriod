package com.hmomeni.canto.vms

import android.arch.lifecycle.ViewModel
import com.hmomeni.canto.di.DIComponent
import com.hmomeni.canto.entities.*
import com.hmomeni.canto.persistence.PostDao
import com.hmomeni.canto.persistence.ProjectDao
import com.hmomeni.canto.persistence.TrackDao
import io.reactivex.Completable
import java.io.File
import javax.inject.Inject

class EditViewModel : ViewModel(), DIComponent.Injectable {
    override fun inject(diComponent: DIComponent) {
        diComponent.inject(this)
    }


    @Inject
    lateinit var projectDao: ProjectDao
    @Inject
    lateinit var postDao: PostDao
    @Inject
    lateinit var trackDao: TrackDao

    fun saveDubsmash(finalFile: File, post: FullPost): Completable {
        return Completable.create {

            postDao.insert(post)

            val project = Project(
                    name = post.name,
                    type = PROJECT_TYPE_DUBSMASH,
                    postId = post.id
            )

            val pId = projectDao.insert(project)

            val videoTrack = Track(
                    projectId = pId.toInt(),
                    type = TRACK_TYPE_FINAL,
                    index = 0,
                    filePath = finalFile.absolutePath
            )

            trackDao.insert(videoTrack)

            it.onComplete()
        }
    }

    fun saveSinging(finalFile: File, post: FullPost): Completable {
        return Completable.create {

            postDao.insert(post)

            val project = Project(
                    name = post.name,
                    type = PROJECT_TYPE_SINGING,
                    postId = post.id
            )

            val pId = projectDao.insert(project)

            val videoTrack = Track(
                    projectId = pId.toInt(),
                    type = TRACK_TYPE_FINAL,
                    index = 0,
                    filePath = finalFile.absolutePath
            )

            trackDao.insert(videoTrack)

            it.onComplete()
        }
    }
}