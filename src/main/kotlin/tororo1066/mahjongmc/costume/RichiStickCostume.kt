package tororo1066.mahjongmc.costume

import tororo1066.displaymonitorapi.configuration.IAdvancedConfigurationSection
import tororo1066.mahjongmc.MahjongMc
import tororo1066.tororopluginapi.SJavaPlugin
import java.io.File

class RichiStickCostume: AbstractCostume() {

    fun getOnRenderRichiStick(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("onRenderRichiStick")
        } ?: emptyList()
    }

    companion object: CostumeFactory<RichiStickCostume>() {
        override fun createDefault(): IAdvancedConfigurationSection {
            return MahjongMc.displayUtils.loadAdvancedConfiguration(
                File(SJavaPlugin.plugin.dataFolder, "costume/richiStick/default.yml")
            )
        }

        override fun createEmpty(): RichiStickCostume {
            return RichiStickCostume()
        }
    }
}