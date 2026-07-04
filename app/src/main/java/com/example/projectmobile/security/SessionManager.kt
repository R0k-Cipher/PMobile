package com.example.projectmobile.security

import android.content.Context

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("secure_session", Context.MODE_PRIVATE)

    fun saveUser(userId: Int, username: String) {
        prefs.edit()
            .putInt("user_id", userId)
            .putString("username", username)
            .putBoolean("logged_in", true)
            .apply()
    }

    fun getUserId(): Int {
        return prefs.getInt("user_id", -1)
    }

    fun getUsername(): String {
        return prefs.getString("username", "") ?: ""
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("logged_in", false)
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}