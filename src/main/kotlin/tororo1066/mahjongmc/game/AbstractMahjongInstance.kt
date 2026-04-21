package tororo1066.mahjongmc.game

import tororo1066.mahjongmc.tile.Tile

abstract class AbstractMahjongInstance {

    abstract fun discardTile(player: PlayerInstance, tile: Tile)
}