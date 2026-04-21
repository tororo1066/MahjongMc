package tororo1066.mahjongmc.mahjong.yaku.yakuman

import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.AbstractWinning
import tororo1066.mahjongmc.mahjong.Meld
import tororo1066.mahjongmc.mahjong.WinningStructure
import tororo1066.mahjongmc.mahjong.yaku.doubleYakuman.ThirteenWaitThirteenOrphans

object ThirteenOrphans: AbstractYakumanWinning() {
    override val name: String = "国士無双"
    override val conflictsWith: Set<AbstractWinning> = setOf(ThirteenWaitThirteenOrphans)

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        return winningStructure.winningTiles.melds.all { it is Meld.Single }
    }
}