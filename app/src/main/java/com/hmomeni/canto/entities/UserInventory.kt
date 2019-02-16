package com.hmomeni.canto.entities

import android.util.SparseIntArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.hmomeni.canto.App
import com.hmomeni.canto.utils.UserSession
import com.pixplicity.easyprefs.library.Prefs
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserInventory @Inject constructor(val userSession: UserSession) {
    var items: SparseIntArray = SparseIntArray()
    var coins: Long = 0

    init {
        val inventory = Prefs.getString("inventory", "")
        if (inventory.isNotEmpty()) {
            val type = object : TypeToken<SparseIntArray>() {}.type
            items = App.gson.fromJson(inventory, type)
        }

        coins = Prefs.getLong("coins", 0)
    }

    fun update(jo: JsonObject) {
        val array = jo["posts"].asJsonArray
        val sparseIntArray = SparseIntArray()
        for (je in array) {
            with(je as JsonObject) {
                sparseIntArray.put(get("id").asInt, get("count").asInt)
            }
        }
        val c = jo["coins"].asLong
        coins = c
        userSession.user?.coins = c.toInt()
        userSession.updateUser()
        items = sparseIntArray
        save()
    }

    fun save() {
        val inventory = App.gson.toJson(items)
        Prefs.putString("inventory", inventory)
        Prefs.putLong("coins", coins)
    }
}