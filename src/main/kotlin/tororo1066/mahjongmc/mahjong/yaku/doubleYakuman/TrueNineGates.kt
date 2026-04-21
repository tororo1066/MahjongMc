package tororo1066.mahjongmc.mahjong.yaku.doubleYakuman

import tororo1066.mahjongmc.Call.Companion.excludeConcealedKan
import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.Meld
import tororo1066.mahjongmc.mahjong.WinningStructure
import tororo1066.mahjongmc.tile.TileType

object TrueNineGates: AbstractDoubleYakumanWinning() {
    override val name: String = "純正九蓮宝燈"

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        if (player.calls.excludeConcealedKan().isNotEmpty()) return false
        val tiles = winningStructure.winningTiles.heads + winningStructure.winningTiles.melds.flatMap {
            if (it is Meld.Quadruplet) it.tiles.take(3) else it.tiles
        }.toMutableList() - winningStructure.winningTile
        if (tiles.any { it.type == TileType.HONORS || it.type != tiles.first().type }) return false

        val counts = IntArray(10)
        for (tile in tiles) {
            counts[tile.number]++
        }
        return counts[1] >= 3 && counts[9] >= 3 && (2..8).all { counts[it] >= 1 }
    }
}