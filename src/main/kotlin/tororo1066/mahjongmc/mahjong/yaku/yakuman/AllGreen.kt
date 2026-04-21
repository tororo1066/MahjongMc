package tororo1066.mahjongmc.mahjong.yaku.yakuman

import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.WinningStructure
import tororo1066.mahjongmc.tile.HonorTiles
import tororo1066.mahjongmc.tile.Tile
import tororo1066.mahjongmc.tile.TileType

object AllGreen: AbstractYakumanWinning() {
    override val name: String = "緑一色"

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        fun isGreen(tile: Tile): Boolean {
            if (tile.honor == HonorTiles.GREEN) return true
            if (tile.type != TileType.BAMBOO) return false
            return when (tile.number) {
                2, 3, 4, 6, 8 -> true
                else -> false
            }
        }

        return winningStructure.winningTiles.tiles.all { isGreen(it) }
    }
}