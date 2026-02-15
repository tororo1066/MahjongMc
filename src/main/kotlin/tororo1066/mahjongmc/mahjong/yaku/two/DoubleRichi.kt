package tororo1066.mahjongmc.mahjong.yaku.two

import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.WinningStructure

object DoubleRichi: AbstractTwoWinning() {
    override val name: String = "ダブル立直"

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        return player.isDoubleRichi
    }
}