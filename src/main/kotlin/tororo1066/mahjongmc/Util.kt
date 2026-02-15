package tororo1066.mahjongmc

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.util.Vector

object Util {

    fun ConfigurationSection.get3Point(path: String): Vector? {
        return this.getString(path)?.let {
            val split = it.split(",")
            Vector(split[0].toDouble(), split[1].toDouble(), split[2].toDouble())
        }
    }

    fun ConfigurationSection.get3PointList(path: String): List<Vector> {
        return this.getStringList(path).map {
            val split = it.split(",")
            Vector(split[0].toDouble(), split[1].toDouble(), split[2].toDouble())
        }
    }

    fun <T> Boolean.nullIfFalse(value: () -> T): T? {
        return if (this) value() else null
    }
}