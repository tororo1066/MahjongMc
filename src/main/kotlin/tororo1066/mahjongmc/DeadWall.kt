package tororo1066.mahjongmc

import tororo1066.mahjongmc.enums.PlayerSettings
import tororo1066.mahjongmc.tile.Tile

class DeadWall {
    val replacementTiles = mutableListOf<Tile>()//嶺上牌 4枚
    val bonusTiles = mutableListOf<Tile>()//ドラ牌 5枚
    val openedBonusTiles = mutableListOf<Tile>()//表ドラ牌
    val actualOpenedBonusTiles = mutableListOf<Tile>()
    val hiddenBonusTiles = mutableListOf<Tile>()//裏ドラ牌 5枚
    val openedHiddenTiles = mutableListOf<Tile>()
    val actualOpenedHiddenTiles = mutableListOf<Tile>()

    val displayTiles: List<Tile>
        get() = openedHiddenTiles + hiddenBonusTiles + openedBonusTiles + bonusTiles

    fun setUpDeadWall(generalTiles: MutableList<Tile>, playerSettings: PlayerSettings) {
        replacementTiles.clear()
        bonusTiles.clear()
        openedBonusTiles.clear()
        actualOpenedBonusTiles.clear()
        hiddenBonusTiles.clear()
        openedHiddenTiles.clear()
        actualOpenedHiddenTiles.clear()

        repeat(4) {
            replacementTiles.add(generalTiles.removeFirst().apply {
                isReplacementTile = true
            })
        }
        repeat(5) {
            bonusTiles.add(generalTiles.removeFirst())
        }
        repeat(5) {
            hiddenBonusTiles.add(generalTiles.removeFirst())
        }

        openBonusTile(playerSettings)
    }

    fun openBonusTile(playerSettings: PlayerSettings): Tile? {
        if (bonusTiles.isEmpty()) return null
        val tile = bonusTiles.removeFirst()
        openedBonusTiles.add(tile)
        actualOpenedBonusTiles.add(tile.getBonusTile(playerSettings))

        val blindTile = hiddenBonusTiles.removeFirst()
        openedHiddenTiles.add(blindTile)
        actualOpenedHiddenTiles.add(blindTile.getBonusTile(playerSettings))

        return tile
    }

    fun isBonusTile(tile: Tile): Boolean {
        return actualOpenedBonusTiles.any { it.isSameTile(tile)  }
    }

    fun isHiddenBonusTile(tile: Tile): Boolean {
        return actualOpenedHiddenTiles.any { it.isSameTile(tile)  }
    }

    fun takeReplacementTile(generalTiles: MutableList<Tile>): Tile? {
        if (replacementTiles.isEmpty() || generalTiles.isEmpty()) return null
        val tile = replacementTiles.removeFirst()
        val newTile = generalTiles.removeLast().apply {
            isReplacementTile = true
        }
        replacementTiles.add(newTile)
        return tile
    }
}