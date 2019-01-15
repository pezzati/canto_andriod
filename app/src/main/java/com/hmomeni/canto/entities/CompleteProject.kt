package com.hmomeni.canto.entities

import androidx.room.Embedded

class CompleteProject(
        val projectId: Long,
        val filePath: String,
        val ratio: Int,
        @Embedded val post: FullPost
)