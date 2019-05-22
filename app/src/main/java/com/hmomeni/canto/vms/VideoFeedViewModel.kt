package com.hmomeni.canto.vms

import androidx.lifecycle.ViewModel
import com.hmomeni.canto.api.Api
import javax.inject.Inject

class VideoFeedViewModel @Inject constructor() : ViewModel() {
    @Inject
    lateinit var api: Api
}