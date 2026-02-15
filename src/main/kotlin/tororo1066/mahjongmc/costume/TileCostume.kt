package tororo1066.mahjongmc.costume

import org.bukkit.configuration.file.YamlConfiguration
import tororo1066.displaymonitorapi.configuration.IAdvancedConfigurationSection
import tororo1066.mahjongmc.MahjongMc
import tororo1066.tororopluginapi.SJavaPlugin
import java.io.File

class TileCostume: AbstractCostume() {

    fun getOnSpawn(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("onSpawn")
        } ?: emptyList()
    }

    fun getOnMove(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("onMove")
        } ?: emptyList()
    }

    fun getOnRender(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("onRender")
        } ?: emptyList()
    }

    fun renderTileState(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("renderTileState")
        } ?: emptyList()
    }

//    fun getOnKuigaeAdd(): List<IAdvancedConfigurationSection> {
//        return getOrDefault {
//            it.getNullableActionList("kuigae.onAdd")
//        } ?: emptyList()
//    }
//
//    fun getOnKuigaeRemove(): List<IAdvancedConfigurationSection> {
//        return getOrDefault {
//            it.getNullableActionList("kuigae.onRemove")
//        } ?: emptyList()
//    }
//
//    fun getOnRichiableAdd(): List<IAdvancedConfigurationSection> {
//        return getOrDefault {
//            it.getNullableActionList("richi.onAdd")
//        } ?: emptyList()
//    }
//
//    fun getOnRichiableAddDenied(): List<IAdvancedConfigurationSection> {
//        return getOrDefault {
//            it.getNullableActionList("richi.onAddDenied")
//        } ?: emptyList()
//    }
//
//    fun getOnRichiableRemove(): List<IAdvancedConfigurationSection> {
//        return getOrDefault {
//            it.getNullableActionList("richi.onRemove")
//        } ?: emptyList()
//    }

    companion object: CostumeFactory<TileCostume> {
        override var default = MahjongMc.displayUtils.loadAdvancedConfiguration(
            File(SJavaPlugin.plugin.dataFolder, "costume/tile/default.yml")
        )

        override fun create(): TileCostume {
            return TileCostume()
        }
    }
}
