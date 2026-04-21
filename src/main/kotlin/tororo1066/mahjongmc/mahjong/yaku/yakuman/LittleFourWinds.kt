package tororo1066.mahjongmc.mahjong.yaku.yakuman

import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.AbstractWinning
import tororo1066.mahjongmc.mahjong.Meld
import tororo1066.mahjongmc.mahjong.WinningStructure
import tororo1066.mahjongmc.mahjong.yaku.doubleYakuman.BigFourWinds
import tororo1066.mahjongmc.tile.HonorTiles

object LittleFourWinds: AbstractYakumanWinning() {
    override val name: String = "小四喜"
    override val conflictsWith: Set<AbstractWinning> = setOf(BigFourWinds)

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        val heads = winningStructure.winningTiles.heads.firstOrNull() ?: return false
        if (!heads.isWind()) return false
        val headsType = heads.honor
        val winds = HonorTiles.WINDS.filter { it != headsType }
        return winds.all { wind ->
            winningStructure.winningTiles.melds.any { meld ->
                meld.tiles.all { it.honor == wind } && (meld is Meld.Triplet || meld is Meld.Quadruplet)
            }
        }
    }
}