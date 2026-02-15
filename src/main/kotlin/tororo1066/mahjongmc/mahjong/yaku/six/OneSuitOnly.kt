package tororo1066.mahjongmc.mahjong.yaku.six

import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.WinningStructure
import tororo1066.mahjongmc.tile.TileType

object OneSuitOnly: AbstractSixWinning() {
    override val name: String = "清一色"

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        val firstTile = winningStructure.winningTiles.tiles.firstOrNull() ?: return false
        if (firstTile.type == TileType.HONORS) return false

        return winningStructure.winningTiles.tiles.all { it.type == firstTile.type }
    }
}