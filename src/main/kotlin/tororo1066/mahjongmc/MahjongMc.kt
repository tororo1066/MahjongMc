package tororo1066.mahjongmc

import kotlinx.coroutines.Job
import kotlinx.coroutines.future.asDeferred
import org.bukkit.configuration.file.YamlConfiguration
import tororo1066.displaymonitor.elements.builtin.DisplayBaseElement
import tororo1066.displaymonitorapi.IDisplayMonitor
import tororo1066.displaymonitorapi.IDisplayUtils
import tororo1066.displaymonitorapi.actions.IActionContext
import tororo1066.displaymonitorapi.actions.IActionRunner
import tororo1066.displaymonitorapi.actions.IPublicActionContext
import tororo1066.displaymonitorapi.configuration.Execute
import tororo1066.displaymonitorapi.configuration.IAdvancedConfiguration
import tororo1066.displaymonitorapi.configuration.IAdvancedConfigurationSection
import tororo1066.displaymonitorapi.storage.IActionStorage
import tororo1066.mahjongmc.command.MahjongCommand
import tororo1066.mahjongmc.dmonitor.workspace.MahjongWorkspace
import tororo1066.tororopluginapi.SJavaPlugin
import java.util.concurrent.CompletableFuture

class MahjongMc: SJavaPlugin(UseOption.SConfig) {

    companion object {
        lateinit var actionStorage: IActionStorage
        lateinit var displayUtils: IDisplayUtils
        lateinit var actionRunner: IActionRunner

        fun emptyAdvancedConfiguration(): IAdvancedConfiguration {
            return displayUtils.toAdvancedConfiguration(YamlConfiguration())
        }

        fun createPublicActionContext(): IPublicActionContext {
            return actionStorage.createPublicContext().apply {
                this.workspace = MahjongWorkspace
            }
        }

        fun createActionContext(publicActionContext: IPublicActionContext = createPublicActionContext()): IActionContext {
            return actionStorage.createActionContext(publicActionContext)
        }

        fun runActions(actions: List<IAdvancedConfigurationSection>, context: IActionContext): Job {
            return actionRunner.run(
                emptyAdvancedConfiguration(),
                actions,
                context,
                null,
                true,
                false
            ).asDeferred()
        }

        fun injectOnInteract(
            publicActionContext: IPublicActionContext,
            name: String,
            onInteract: (IActionContext) -> Unit
        ) {
            val element = findDisplayElement(publicActionContext, name) ?: return
            element.onInteract = Execute { context ->
                onInteract(context)
            }
        }

        fun injectOnHover(
            publicActionContext: IPublicActionContext,
            name: String,
            onHover: (IActionContext) -> CompletableFuture<Unit>
        ) {
            val element = findDisplayElement(publicActionContext, name) ?: return
            element.onHover = Execute { context ->
                onHover(context)
            }
        }

        fun injectOnUnhover(
            publicActionContext: IPublicActionContext,
            name: String,
            onUnhover: (IActionContext) -> CompletableFuture<Unit>
        ) {
            val element = findDisplayElement(publicActionContext, name) ?: return
            element.onUnhover = Execute { context ->
                onUnhover(context)
            }
        }

        private fun findDisplayElement(
            publicActionContext: IPublicActionContext,
            name: String
        ): DisplayBaseElement? {
            return publicActionContext.allElements.entries.find {
                it.key.contains(name) && it.value is DisplayBaseElement
            }?.value as? DisplayBaseElement
        }
    }

    override fun onStart() {
        val displayMonitor = IDisplayMonitor.DisplayMonitorInstance.getInstance()
        actionStorage = displayMonitor.actionStorage
        displayUtils = displayMonitor.utils
        actionRunner = displayMonitor.actionRunner
        MahjongCommand()
    }

    override fun onEnd() {

    }
}