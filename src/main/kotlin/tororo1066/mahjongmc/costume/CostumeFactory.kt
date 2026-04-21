package tororo1066.mahjongmc.costume

import tororo1066.displaymonitorapi.configuration.IAdvancedConfigurationSection

abstract class CostumeFactory<T: AbstractCostume> {

    companion object {
        val factories = mutableListOf<CostumeFactory<*>>()

        fun reloadDefaults() {
            factories.forEach { it.reloadDefault() }
        }
    }

    var default: IAdvancedConfigurationSection = createDefault()

    init {
        factories.add(this)
    }

    fun reloadDefault() {
        default = createDefault()
    }

    abstract fun createDefault(): IAdvancedConfigurationSection

    abstract fun createEmpty(): T

    fun create(config: IAdvancedConfigurationSection): T {
        val costume = createEmpty()
        costume.loadConfig(config)
        return costume
    }
}