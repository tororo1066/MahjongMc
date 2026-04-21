package tororo1066.mahjongmc.costume

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

    fun getOnHover(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("onHover")
        } ?: emptyList()
    }

    fun getOnUnhover(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("onUnhover")
        } ?: emptyList()
    }

    fun renderTileState(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("renderTileState")
        } ?: emptyList()
    }

    companion object: CostumeFactory<TileCostume>() {
        override fun createDefault(): IAdvancedConfigurationSection {
            return MahjongMc.displayUtils.loadAdvancedConfiguration(
                File(SJavaPlugin.plugin.dataFolder, "costume/tile/default.yml")
            )
        }

        override fun createEmpty(): TileCostume {
            return TileCostume()
        }
    }
}
