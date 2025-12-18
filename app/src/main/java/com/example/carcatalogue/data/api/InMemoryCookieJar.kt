package com.example.carcatalogue.data.api

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class InMemoryCookieJar : CookieJar {
    private val cookieStore = mutableListOf<Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        // Replace cookies with same name/domain/path
        cookies.forEach { newCookie ->
            cookieStore.removeAll { existing ->
                existing.name == newCookie.name &&
                    existing.domain == newCookie.domain &&
                    existing.path == newCookie.path
            }
            cookieStore.add(newCookie)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        cookieStore.removeAll { it.expiresAt < now }
        return cookieStore.filter { it.matches(url) }
    }
}
