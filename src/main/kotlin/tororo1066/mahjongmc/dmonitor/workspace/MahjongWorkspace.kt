package tororo1066.mahjongmc.dmonitor.workspace

import tororo1066.displaymonitorapi.configuration.IActionConfiguration
import tororo1066.displaymonitorapi.workspace.IAbstractWorkspace

object MahjongWorkspace: IAbstractWorkspace {

    private val actionConfigurations: MutableMap<String, IActionConfiguration> = mutableMapOf()

    override fun getName(): String {
        return "Mahjong_Workspace"
    }

    override fun getActionConfigurations(): Map<String, IActionConfiguration> {
        return actionConfigurations
    }
}