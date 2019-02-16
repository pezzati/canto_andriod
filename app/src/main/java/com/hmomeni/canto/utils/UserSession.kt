package com.hmomeni.canto.utils

import com.hmomeni.canto.entities.User
import com.hmomeni.canto.persistence.UserDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSession @Inject constructor(val userDao: UserDao) {
    var user: User? = null
    var token: String? = null

    fun isUser() = token != null

    fun updateUser() {
        user?.let {
            userDao.updateUser(it)
        }
    }
}