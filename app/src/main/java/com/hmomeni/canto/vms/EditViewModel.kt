package com.hmomeni.canto.vms

import androidx.lifecycle.ViewModel
import com.hmomeni.canto.App
import com.hmomeni.canto.api.Api
import com.hmomeni.canto.di.DIComponent
import com.hmomeni.canto.entities.*
import com.hmomeni.canto.persistence.PostDao
import com.hmomeni.canto.persistence.ProjectDao
import com.hmomeni.canto.persistence.TrackDao
import io.reactivex.Completable
import io.reactivex.Single
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import javax.inject.Inject

class EditViewModel() : ViewModel(), DIComponent.Injectable {
    override fun inject(diComponent: DIComponent) {
        diComponent.inject(this)
    }

    constructor(app: App) : this() {
        app.di.inject(this)
    }

    @Inject
    lateinit var projectDao: ProjectDao
    @Inject
    lateinit var postDao: PostDao
    @Inject
    lateinit var trackDao: TrackDao
    @Inject
    lateinit var api: Api

    fun getPost(postId: Long): Single<FullPost> = postDao.getPost(postId.toLong())
            .onErrorResumeNext {
                api.getSinglePost(postId.toInt())
            }

    fun uploadSong(filePath: String, postId: Long): Completable {
        val file = File(filePath)

        val map = HashMap<String, RequestBody>()
        map["name"] = RequestBody.create(MediaType.parse("text/plain"), "")
        map["karaoke"] = RequestBody.create(MediaType.parse("text/plain"), postId.toString())
        val requestFile = RequestBody.create(MediaType.parse("video/mp4"), file)
        val part = MultipartBody.Part.createFormData("file", file.name, requestFile)
        return api.uploadSong(part, map)
    }

    fun saveDubsmash(finalFile: String, post: FullPost, ratio: Int): Completable {
        return Completable.create {

            postDao.insertIgnore(post)

            val project = Project(
                    name = post.name,
                    type = PROJECT_TYPE_DUBSMASH,
                    postId = post.id
            )

            val pId = projectDao.insert(project)

            val videoTrack = Track(
                    projectId = pId,
                    type = TRACK_TYPE_FINAL,
                    index = 0,
                    filePath = finalFile,
                    ratio = ratio
            )

            trackDao.insert(videoTrack)

            it.onComplete()
        }
    }

    fun saveSinging(finalFile: String, post: FullPost, ratio: Int): Completable {
        return Completable.create {

            postDao.insertIgnore(post)

            val project = Project(
                    name = post.name,
                    type = PROJECT_TYPE_SINGING,
                    postId = post.id
            )

            val pId = projectDao.insert(project)

            val videoTrack = Track(
                    projectId = pId,
                    type = TRACK_TYPE_FINAL,
                    index = 0,
                    filePath = finalFile,
                    ratio = ratio
            )

            trackDao.insert(videoTrack)

            it.onComplete()
        }
    }
}