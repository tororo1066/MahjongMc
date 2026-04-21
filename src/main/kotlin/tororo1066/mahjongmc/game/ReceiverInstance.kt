package tororo1066.mahjongmc.game

import tororo1066.mahjongmc.tile.Tile
import java.util.UUID

open class ReceiverInstance {

    val otherPlayerDisplayTiles = mutableMapOf<UUID, MutableList<Tile>>()
    val otherPlayerDisplayDiscard = mutableMapOf<UUID, MutableList<Tile>>()
}