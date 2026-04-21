package tororo1066.mahjongmc.mahjong.yaku.yakuman

import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.WinningStructure

object HeavenlyHand: AbstractYakumanWinning() {
    override val name: String = "天和"

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        if (!isTsumo) return false
        if (instance.players.any { it.discard.isNotEmpty() || it.calls.isNotEmpty() }) return false
        return instance.dealerPosition == player.position
    }
}