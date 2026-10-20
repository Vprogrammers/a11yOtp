package me.vprogrammers.a11yotp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
