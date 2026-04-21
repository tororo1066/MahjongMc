package tororo1066.mahjongmc.mahjong.yaku.yakuman

import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.WinningStructure

object EarthlyHand: AbstractYakumanWinning() {
    override val name: String = "地和"

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        if (!isTsumo) return false
        if (instance.players.any { it.calls.isNotEmpty() || it.discard.size > 1 }) return false
        if (instance.players.all { it.discard.isNotEmpty() }) return false
        return instance.dealerPosition != player.position
    }
}