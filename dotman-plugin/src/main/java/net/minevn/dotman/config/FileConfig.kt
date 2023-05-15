package net.minevn.dotman.config

import net.minevn.dotman.DotMan
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

open class FileConfig(val name: String) {
	protected val file: File
	protected val main = DotMan.instance
	lateinit var config: YamlConfiguration
		private set

	init {
		file = File(main.dataFolder, "$name.yml")
		if (!file.exists()) {
			file.parentFile.mkdirs()
			main.saveResource("$name.yml", false)
		}
		initYaml()
	}

	protected fun initYaml() {
		config = YamlConfiguration.loadConfiguration(file)
	}

	open fun reload() {
		initYaml()
	}

	open fun save() {
		config.save(file)
	}
}