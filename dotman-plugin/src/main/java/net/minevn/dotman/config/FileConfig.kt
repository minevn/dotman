package net.minevn.dotman.config

import net.minevn.dotman.DotMan
import net.minevn.libs.bukkit.color
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

open class FileConfig(val name: String) {
    private val file: File
    protected val main = DotMan.instance
    lateinit var config: YamlConfiguration private set
    lateinit var baseConfig: YamlConfiguration private set

    init {
        file = File(main.dataFolder, "$name.yml")
        if (!file.exists()) {
            file.parentFile.mkdirs()
            main.saveResource("$name.yml", false)
        }
        initYaml()
    }

    private fun initYaml() {
        config = YamlConfiguration.loadConfiguration(file)
        main.getResource("$name.yml").use {
            baseConfig = YamlConfiguration.loadConfiguration(InputStreamReader(it, StandardCharsets.UTF_8))
        }
    }

    fun get(key: String): String = (config.getString(key) ?: baseConfig.getString(key, "")).color()

    fun getList(key: String) = (config.getStringList(key)?.takeIf { it.isNotEmpty() } ?: baseConfig.getStringList(key))
        .map { it.replace("%PREFIX%", MainConfig.get().prefix) }
        .color()

    open fun reload() {
        initYaml()
    }

    open fun save() {
        config.save(file)
    }
}