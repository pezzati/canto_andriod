package com.hmomeni.canto.fragments

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.crashlytics.android.Crashlytics
import com.hmomeni.canto.R
import com.hmomeni.canto.activities.ShopActivity
import com.hmomeni.canto.adapters.rcl.MainRclAdapter
import com.hmomeni.canto.entities.Banner
import com.hmomeni.canto.entities.Genre
import com.hmomeni.canto.entities.Post
import com.hmomeni.canto.utils.*
import com.hmomeni.canto.utils.navigation.ListNavEvent
import com.hmomeni.canto.utils.navigation.PostNavEvent
import com.hmomeni.canto.vms.MainViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_main.*
import retrofit2.HttpException
import timber.log.Timber

class MainFragment : Fragment() {
    private lateinit var viewModel: MainViewModel
    private val compositeDisposable = CompositeDisposable()

    private var adapter: MainRclAdapter? = null

    private val genres: MutableList<Genre> = mutableListOf()
    private val banners: MutableList<Banner> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, ViewModelFactory(context!!.app()))[MainViewModel::class.java]
        viewModel.api.getBanners()
                .map { it.data }
                .iomain()
                .subscribe({
                    banners.addAll(it)
                    adapter?.notifyDataSetChanged()
                }, {
                    Timber.e(it, "Failed loading banners")
                }).addTo(compositeDisposable)

        viewModel.api.getHomeFeed()
                .iomain()
                .doAfterTerminate {
                    progressBar?.visibility = View.GONE
                }
                .subscribe({
                    it.forEach { f ->
                        val genre = Genre(
                                filesLink = f.moreUrl,
                                link = f.moreUrl,
                                name = f.name,
                                posts = f.posts
                        )
                        genres.add(genre)
                    }
                    adapter?.notifyDataSetChanged()
                }, {
                    Timber.e(it, "Failed loading genres")
                }).addTo(compositeDisposable)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (adapter == null) {
            adapter = MainRclAdapter(banners, genres).also {
                it.clickPublisher.subscribe {
                    when (it.type) {
                        MainRclAdapter.ClickEvent.Type.BANNER -> {
                        }
                        MainRclAdapter.ClickEvent.Type.GENRE -> {
                            val pos = it.row - 1
                            val genre = genres[pos]
                            if (it.item == -1) {
                                viewModel.navEvents.onNext(ListNavEvent("url_path", 0, genre.name, genre.filesLink))
                            } else {
                                val post = genre.posts!![it.item]
                                val dialog = ProgressDialog(context!!)
                                viewModel
                                        .sing(post)
                                        .iomain()
                                        .doOnSubscribe { dialog.show() }
                                        .doAfterTerminate { dialog.dismiss() }
                                        .subscribe({
                                            viewModel.navEvents.onNext(PostNavEvent(post))
                                        }, {
                                            if (it is HttpException) {
                                                when (it.code()) {
                                                    HTTP_ERROR_PAYMENT_REQUIRED -> {
                                                        PaymentDialog(context!!, showNegativeButton = true, positiveListener = {
                                                            startActivity(Intent(context, ShopActivity::class.java))
                                                        }).show()
                                                    }
                                                    HTTP_ERROR_NOT_PURCHASED -> {
                                                        PaymentDialog(
                                                                context!!,
                                                                title = getString(R.string.purchase_song),
                                                                content = getString(R.string.are_you_sure_to_but_x_tries, 5, post.name),
                                                                imageUrl = post.artist?.image,
                                                                showNegativeButton = true,
                                                                positiveButtonText = getString(R.string.yes_buy),
                                                                positiveListener = {
                                                                    purchaseSong(post)
                                                                }
                                                        ).show()
                                                    }
                                                }
                                            }
                                            Timber.e(it)
                                        }).addTo(compositeDisposable)
                            }

                        }
                    }
                }.addTo(compositeDisposable)
            }
        } else {
            progressBar.visibility = View.GONE
        }

        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)
        recyclerView.adapter = adapter

    }

    private fun purchaseSong(post: Post) {
        val dialog = ProgressDialog(context!!)
        viewModel.purchaseSong(post)
                .iomain()
                .doOnSubscribe { dialog.show() }
                .doAfterTerminate { dialog.dismiss() }
                .subscribe({
                    viewModel.navEvents.onNext(PostNavEvent(post))
                }, {
                    if (it is HttpException && it.code() == HTTP_ERROR_PAYMENT_REQUIRED) {
                        PaymentDialog(context!!, showNegativeButton = true, positiveListener = {
                            startActivity(Intent(context, ShopActivity::class.java))
                        }).show()
                    } else {
                        Toast.makeText(context, R.string.purchase_song_failed, Toast.LENGTH_SHORT).show()
                        Crashlytics.logException(it)
                    }
                }).addTo(compositeDisposable)
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }
}