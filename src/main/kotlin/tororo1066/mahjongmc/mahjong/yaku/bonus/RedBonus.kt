package tororo1066.mahjongmc.mahjong.yaku.bonus

import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.AbstractWinning
import tororo1066.mahjongmc.mahjong.WinningStructure

object RedBonus: AbstractWinning() {
    override val name: String = "赤ドラ"
    override val hasYaku: Boolean = false

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        return player.allHandTiles.any { it.isRed }
    }

    override fun getHan(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Int {
        return player.allHandTiles.count { it.isRed }
    }
}