package com.example.disastermesh.data

import android.content.Context
import android.content.SharedPreferences
import com.example.disastermesh.model.Authority

class AuthorityManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("authority", Context.MODE_PRIVATE)
    
    fun setAuthority(authority: Authority) {
        prefs.edit().putString("current_authority", authority.name).apply()
    }
    
    fun getAuthority(): Authority {
        val authorityName = prefs.getString("current_authority", Authority.USER.name)
        return try {
            Authority.valueOf(authorityName ?: Authority.USER.name)
        } catch (e: Exception) {
            Authority.USER
        }
    }
    
    fun isAdmin(): Boolean = getAuthority() == Authority.ADMIN
    fun isUser(): Boolean = getAuthority() == Authority.USER
}
