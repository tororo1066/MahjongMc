package tororo1066.mahjongmc.mahjong.yaku.one

import tororo1066.mahjongmc.Call.Companion.excludeConcealedKan
import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.WinningStructure

object Tsumo: AbstractOneWinning() {
    override val name: String = "門前清自摸和"

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        return isTsumo && player.calls.excludeConcealedKan().isEmpty()
    }
}