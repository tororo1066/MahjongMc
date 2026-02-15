package tororo1066.mahjongmc.game

import org.bukkit.Location
import tororo1066.mahjongmc.game.PlayerInstance

interface IMahjongInstance {

    val centerLocation: Location
    val players: MutableList<PlayerInstance>
}