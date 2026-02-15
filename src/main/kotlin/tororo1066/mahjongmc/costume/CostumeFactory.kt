package tororo1066.mahjongmc.costume

import tororo1066.displaymonitorapi.configuration.IAdvancedConfigurationSection

interface CostumeFactory<T: AbstractCostume> {
    val default: IAdvancedConfigurationSection

    fun create(): T

    fun create(config: IAdvancedConfigurationSection): T {
        val costume = create()
        costume.loadConfig(config)
        return costume
    }
}