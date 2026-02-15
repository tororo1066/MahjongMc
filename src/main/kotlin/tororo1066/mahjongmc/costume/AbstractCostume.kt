package tororo1066.mahjongmc.costume

import tororo1066.displaymonitorapi.configuration.IAdvancedConfigurationSection
import tororo1066.mahjongmc.game.MahjongInstance
import kotlin.reflect.full.companionObjectInstance

abstract class AbstractCostume {
    private var _config: IAdvancedConfigurationSection? = null
    private val factory = this::class.companionObjectInstance as CostumeFactory<*>

    open fun init(instance: MahjongInstance) {}

    fun loadConfig(config: IAdvancedConfigurationSection) {
        this._config = config
    }

    fun getDefaultConfig(): IAdvancedConfigurationSection {
        return factory.default
    }

    protected fun <T> getOrDefault(value: (IAdvancedConfigurationSection) -> T?): T? {
        val config = _config
        var result: T? = null
        if (config != null) {
            result = value(config)
        }
        if (result == null) {
            result = value(getDefaultConfig())
        }

        return result
    }

    protected fun IAdvancedConfigurationSection.getNullableActionList(path: String): List<IAdvancedConfigurationSection>? {
        return if (this.isList(path)) {
            this.getAdvancedConfigurationSectionList(path)
        } else {
            null
        }
    }
}