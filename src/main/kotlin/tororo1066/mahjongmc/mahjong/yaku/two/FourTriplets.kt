package tororo1066.mahjongmc.mahjong.yaku.two

import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.Meld
import tororo1066.mahjongmc.mahjong.WinningStructure

object FourTriplets: AbstractTwoWinning() {
    override val name: String = "対々和"

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        val melds = winningStructure.winningTiles.melds

        // 4面子すべてが刻子/槓子
        return melds.all { meld ->
            meld is Meld.Triplet || meld is Meld.Quadruplet
        }
    }
}