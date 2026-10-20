package me.vprogrammers.a11yotp

class Greeting {
    private val platform = getPlatform()

    fun greet(): String = sayHello(platform.name)
}
