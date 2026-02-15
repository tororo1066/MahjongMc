package tororo1066.mahjongmc.mahjong

import tororo1066.mahjongmc.tile.Tile

data class WinningStructure(
    val winningTiles: WinningTiles,
    val winningTile: Tile
) {

    fun getWaitType(): WaitType? {
        val melds = winningTiles.melds
        val head = winningTiles.heads.first()
        // 単騎待ち
        if (melds.all { meld -> !meld.tiles.contains(winningTile) } && head.isSameTile(winningTile)) {
            return WaitType.TANKI
        }
        // 双碰待ち
        if (melds.filterIsInstance<Meld.Triplet>().any { meld ->
            meld.tiles.contains(winningTile)
        }) {
            return WaitType.SHANPON
        }

        // 辺張待ち・嵌張待ち・両面待ち
        val sequenceMeld = melds.filterIsInstance<Meld.Sequence>().find { meld ->
            meld.tiles.contains(winningTile)
        } ?: return null
        val index = sequenceMeld.tiles.indexOf(winningTile)
        return when (index) {
            1 -> WaitType.KANCHAN
            0, 2 -> {
                // 辺張待ちか両面待ちかの判定
                val firstTile = sequenceMeld.tiles[0]
                when (firstTile.number) {
                    1, 7 -> WaitType.PENCHAN
                    else -> WaitType.RYANMEN
                }
            }
            else -> null
        }
    }
}