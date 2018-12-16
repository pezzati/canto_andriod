package com.hmomeni.canto.entities

import android.arch.persistence.room.Embedded

class CompleteProject(
        val projectId: Long,
        val filePath: String,
        val ratio: Int,
        @Embedded val post: FullPost
)