package me.bilolib.bobcase.util

import me.bilolib.bobcase.LangManager

object Lang {
    fun get(key: String): String {
        return LangManager.msg(key)
    }
}