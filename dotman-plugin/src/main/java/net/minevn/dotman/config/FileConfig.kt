package net.minevn.dotman.config

import net.minevn.dotman.DotMan
import net.minevn.libs.bukkit.FileConfig

open class FileConfig(fileName: String) : FileConfig(DotMan.instance, fileName) {
    val main = DotMan.instance

    override fun get(key: String) = super.get(key).replace("%PREFIX%", main.prefix)

    override fun getList(key: String) = super.getList(key).map { it.replace("%PREFIX%", main.prefix) }
}
