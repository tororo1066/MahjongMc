package tororo1066.mahjongmc.mahjong.yaku.one

import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.WinningStructure

object RobbingAQuad: AbstractOneWinning() {
    override val name: String = "搶槓"

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        return winningStructure.winningTile.isSelfKanTile
    }
}