package com.meeting.accesscontrol.net

import okhttp3.Headers

object CookieUtils {
    fun getJsessionId(headers: Headers): String? {
        return headers.values("Set-Cookie")
            .firstOrNull { it.startsWith("JSESSIONID=") }
            ?.substringAfter("JSESSIONID=")
            ?.substringBefore(";")
    }
}
