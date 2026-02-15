package tororo1066.mahjongmc.costume

import tororo1066.displaymonitorapi.configuration.IAdvancedConfigurationSection
import tororo1066.mahjongmc.MahjongMc
import tororo1066.tororopluginapi.SJavaPlugin
import java.io.File

class EffectCostume: AbstractCostume() {

    fun getRichiEffects(): List<IAdvancedConfigurationSection> {
        return getOrDefault { config ->
            config.getNullableActionList("action.richi")
        } ?: emptyList()
    }

    fun getDoubleRichiEffects(): List<IAdvancedConfigurationSection> {
        return getOrDefault { config ->
            config.getNullableActionList("action.doubleRichi")
        } ?: emptyList()
    }

    fun getTsumoEffects(): List<IAdvancedConfigurationSection> {
        return getOrDefault { config ->
            config.getNullableActionList("action.tsumo")
        } ?: emptyList()
    }

    fun getRonEffectsCall(): List<IAdvancedConfigurationSection> {
        return getOrDefault { config ->
            config.getNullableActionList("action.ron.call")
        } ?: emptyList()
    }

    fun getRonEffects(): List<IAdvancedConfigurationSection> {
        return getOrDefault { config ->
            config.getNullableActionList("action.ron.final")
        } ?: emptyList()
    }

    companion object: CostumeFactory<EffectCostume> {
        override var default = MahjongMc.displayUtils.loadAdvancedConfiguration(
            File(SJavaPlugin.plugin.dataFolder, "costume/effect/default.yml")
        )

        override fun create(): EffectCostume {
            return EffectCostume()
        }
    }
}