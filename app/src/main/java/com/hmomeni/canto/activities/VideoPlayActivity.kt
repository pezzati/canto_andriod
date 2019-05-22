package com.hmomeni.canto.activities

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.view.SurfaceHolder
import android.view.View
import androidx.core.content.FileProvider
import com.crashlytics.android.Crashlytics
import com.hmomeni.canto.R
import com.hmomeni.canto.entities.FullPost
import com.hmomeni.canto.entities.Project
import com.hmomeni.canto.entities.Track
import com.hmomeni.canto.persistence.PostDao
import com.hmomeni.canto.persistence.ProjectDao
import com.hmomeni.canto.persistence.TrackDao
import com.hmomeni.canto.utils.*
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.activity_video_play.*
import timber.log.Timber
import java.io.File
import java.net.URLConnection
import javax.inject.Inject


class VideoPlayActivity : BaseFullActivity(), View.OnClickListener {
    @Inject
    lateinit var projectDao: ProjectDao
    @Inject
    lateinit var postDao: PostDao
    @Inject
    lateinit var trackDao: TrackDao

    var filePath: String? = null

    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app().di.inject(this)
        setContentView(R.layout.activity_video_play)

        val args = VideoPlayActivityArgs.fromBundle(intent.extras!!)
        disposable = projectDao
                .getProject(args.project.toLong())
                .flatMap { p ->
                    Single.create<Pair<Project, FullPost>> { e -> postDao.getPost(p.postId).subscribe(Consumer { e.onSuccess(Pair(p, it)) }) }
                }
                .flatMap { p ->
                    Single.create<Triple<Project, FullPost, Track>> { e -> trackDao.fetchFinalTrackForProject(p.first.id!!).subscribe(Consumer { e.onSuccess(Triple(p.first, p.second, it)) }) }
                }
                .iomain()
                .subscribe({
                    prepareView(it)
                }, {
                    Timber.e(it)
                    Crashlytics.logException(it)
                })

        gradientView.setOnClickListener(this)
        fastForward.setOnClickListener(this)
        fastForwardText.setOnClickListener(this)
        fastBack.setOnClickListener(this)
        fastBackText.setOnClickListener(this)
        shareBtn.setOnClickListener(this)
        backBtn.setOnClickListener(this)

    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.backBtn -> {
                onBackPressed()
            }
            R.id.gradientView -> if (mediaPlayer.isPlaying) {
                pause()
            } else {
                resume()
            }
            R.id.fastBack, R.id.fastBackText -> jump(false)
            R.id.fastForward, R.id.fastForwardText -> jump(true)
            R.id.shareBtn -> {
                filePath?.let {
                    val file = File(filePath)

                    val fileUri = FileProvider.getUriForFile(this, "com.hmomeni.canto.fileprovider", file)
                    val intentShareFile = Intent(Intent.ACTION_SEND)
                    intentShareFile.type = URLConnection.guessContentTypeFromName(file.name)
                    intentShareFile.putExtra(Intent.EXTRA_STREAM, fileUri)
                    startActivity(Intent.createChooser(intentShareFile, "Share CantoFile"))
                }
            }
        }
    }

    override fun onStop() {
        pause()
        super.onStop()
    }

    override fun onDestroy() {
        cleanUp()
        super.onDestroy()
    }

    val mediaPlayer = MediaPlayer()
    private fun prepareView(triple: Triple<Project, FullPost, Track>) {
        progressBar.visibility = View.GONE
        val post = triple.second
        val track = triple.third

        filePath = track.filePath
        artistName.text = post.artist?.name
        trackName.text = post.name

        GlideApp.with(this)
                .load(post.coverPhoto?.link)
                .placeholder(R.drawable.post_placeholder)
                .rounded(dpToPx(5))
                .into(artistPhoto)

        try {
            mediaPlayer.setDataSource(track.filePath)
            mediaPlayer.setOnPreparedListener {
                resume()
            }
            mediaPlayer.isLooping = true
            mediaPlayer.prepareAsync()
            if (surfaceView.holder.surface.isValid) {
                mediaPlayer.setSurface(surfaceView.holder.surface)
            } else {
                surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {

                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                    }

                    override fun surfaceCreated(holder: SurfaceHolder) {
                        Timber.d("Surface Ready!")
                        mediaPlayer.setSurface(holder.surface)
                    }
                })
            }
        } catch (e: Exception) {
            Timber.e(e)
            Crashlytics.setString("file_path", track.filePath)
            Crashlytics.logException(e)
        }
    }

    private fun pause() {
        mediaPlayer.pause()
    }

    private fun resume() {
        mediaPlayer.start()
        timer()
    }

    private fun cleanUp() {
        disposable?.dispose()
        mediaPlayer.stop()
        mediaPlayer.release()
    }

    private val handler = Handler()
    private fun timer() {
        if (!isDestroyed) {
            val time = mediaPlayer.currentPosition / 1000
            timerText.text = "%02d:%02d".format(time / 60, time % 60)
            if (mediaPlayer.isPlaying) {
                handler.postDelayed({ timer() }, 500)
            }
        }
    }

    private fun jump(forward: Boolean) {
        val time = mediaPlayer.currentPosition
        if (forward) {
            if (time + 10000 < mediaPlayer.duration) {
                mediaPlayer.seekTo(time + 10000)
            }
        } else {
            if (time - 10000 > 0) {
                mediaPlayer.seekTo(time - 10000)
            } else {
                mediaPlayer.seekTo(0)
            }
        }
    }

}