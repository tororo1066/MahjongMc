package tororo1066.mahjongmc.mahjong.yaku.doubleYakuman

import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.WinningStructure
import tororo1066.mahjongmc.mahjong.yaku.yakuman.ThirteenOrphans

object ThirteenWaitThirteenOrphans: AbstractDoubleYakumanWinning() {
    override val name: String = "国士無双十三面待ち"

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        if (!ThirteenOrphans.check(instance, player, winningStructure, isTsumo)) return false
        return winningStructure.winningTiles.tiles.count { it.isSameTile(winningStructure.winningTile) } == 2
    }
}