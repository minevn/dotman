package net.minevn.dotman.test.utils

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import net.minevn.dotman.DotMan
import java.io.File
import java.io.FileInputStream

fun Any.setInternal(fieldName: String, value: Any) {
    val clazz = this.javaClass
    val field = runCatching { clazz.getDeclaredField(fieldName) }.getOrNull()
        ?: clazz.superclass.getDeclaredField(fieldName)
    field.isAccessible = true
    field.set(this, value)
}

fun <T> setStatic(type: Class<T>, fieldName: String, value: T) {
    val field = type.getDeclaredField(fieldName)
    field.isAccessible = true
    field.set(null, value)
}

fun <T> setInstance(type: Class<T>, instance: T) = setStatic(type, "instance", instance)

fun setDotManInstance(instance: DotMan) = setInstance(DotMan::class.java, instance)

fun mockDotMan() = setDotManInstance(mockk {
    every { server } returns mockk {
        every { pluginManager } returns mockk {
            every { disablePlugin(any()) } just runs
        }
    }

    every { dataFolder } returns File("../testdir")

    every { getResource(any<String>()) } answers {
        runCatching { FileInputStream(File("src/main/resources/${firstArg<String>()}")) }.getOrNull()
    }

    every { logger } returns mockk {
        every { info(any<String>()) } answers { println("LOG: ${firstArg<String>()}") }
        every { log(any(), any<String>(), any<Exception>()) } answers {
            thirdArg<Exception>().printStackTrace()
        }
    }
})