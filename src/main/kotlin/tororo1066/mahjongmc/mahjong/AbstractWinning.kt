package tororo1066.mahjongmc.mahjong

import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance

abstract class AbstractWinning {

    abstract val name: String
    open val hasYaku: Boolean = true
    open val yakumanValue: Int = 0

    abstract fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean

    abstract fun getHan(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Int

    open fun conflictsWith(other: AbstractWinning): Boolean {
        return false
    }
}