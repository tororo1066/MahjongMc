package tororo1066.mahjongmc.config

import org.bukkit.configuration.file.YamlConfiguration

object PublicConfig {

    var DISCARD_WAIT_TIME = 100L

    fun init(config: YamlConfiguration) {
        DISCARD_WAIT_TIME = config.getLong("discardWaitTime", 100)
    }
}