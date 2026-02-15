package tororo1066.mahjongmc.mahjong

import tororo1066.mahjongmc.tile.Tile

data class WinningTiles(
    val heads: List<Tile>,
    val melds: List<Meld>
) {
    val tiles: List<Tile>
        get() {
            val result = mutableListOf<Tile>()
            melds.forEach { meld ->
                result.addAll(meld.tiles)
            }
            result.addAll(heads)
            return result
        }
}