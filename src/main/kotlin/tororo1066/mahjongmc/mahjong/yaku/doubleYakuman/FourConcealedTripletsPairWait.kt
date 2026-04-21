package tororo1066.mahjongmc.mahjong.yaku.doubleYakuman

import tororo1066.mahjongmc.mahjong.WaitType
import tororo1066.mahjongmc.mahjong.yaku.yakuman.FourConcealedTriplets

object FourConcealedTripletsPairWait: AbstractDoubleYakumanWinning() {
    override val name: String = "四暗刻単騎"

    override fun check(
        instance: tororo1066.mahjongmc.game.MahjongInstance,
        player: tororo1066.mahjongmc.game.PlayerInstance,
        winningStructure: tororo1066.mahjongmc.mahjong.WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        if (!FourConcealedTriplets.check(instance, player, winningStructure, isTsumo)) return false
        return winningStructure.getWaitType() == WaitType.TANKI
    }
}