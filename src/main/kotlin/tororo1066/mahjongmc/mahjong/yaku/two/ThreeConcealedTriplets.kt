package tororo1066.mahjongmc.mahjong.yaku.two

import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.Meld
import tororo1066.mahjongmc.mahjong.WinningStructure

object ThreeConcealedTriplets: AbstractTwoWinning() {
    override val name: String = "三暗刻"

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        val melds = winningStructure.winningTiles.melds

        // 3面子が暗刻/暗槓であること
        val concealedTriplets = melds.count { meld ->
            (meld is Meld.Triplet || meld is Meld.Quadruplet) && !meld.isCalled
        }
        return concealedTriplets >= 3
    }
}