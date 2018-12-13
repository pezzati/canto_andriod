package com.hmomeni.canto.utils

import com.hmomeni.canto.entities.User

class UserSession(
        var user: User? = null
) {
    fun isUser() = user != null
}