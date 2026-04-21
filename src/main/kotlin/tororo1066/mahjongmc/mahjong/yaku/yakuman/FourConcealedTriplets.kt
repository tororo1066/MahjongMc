package tororo1066.mahjongmc.mahjong.yaku.yakuman

import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.AbstractWinning
import tororo1066.mahjongmc.mahjong.Meld
import tororo1066.mahjongmc.mahjong.WinningStructure
import tororo1066.mahjongmc.mahjong.yaku.doubleYakuman.FourConcealedTripletsPairWait

object FourConcealedTriplets: AbstractYakumanWinning() {
    override val name: String = "四暗刻"
    override val conflictsWith: Set<AbstractWinning> = setOf(FourConcealedTripletsPairWait)

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        return winningStructure.winningTiles.melds.all { meld ->
            (meld is Meld.Triplet || meld is Meld.Quadruplet) && !meld.isCalled
        }
    }
}