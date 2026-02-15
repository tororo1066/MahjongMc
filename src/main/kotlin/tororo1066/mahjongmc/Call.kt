package tororo1066.mahjongmc

import tororo1066.mahjongmc.enums.Position
import tororo1066.mahjongmc.tile.Tile

class Call() {
    val tiles = mutableListOf<Tile>()
    lateinit var calledTile: Tile
    lateinit var type: Type
    lateinit var target: Position

    constructor(type: Type, tiles: List<Tile>, target: Position, calledTile: Tile) : this() {
        this.type = type
        this.tiles.addAll(tiles)
        this.calledTile = calledTile
        this.target = target

        this.tiles.sortBy {
            if (it == calledTile) {
                0
            } else {
                1
            }
        }
    }

    enum class Type {
        PON,
        CHI,
        KAN,
        LATE_KAN,
        CONCEALED_KAN
    }

    fun isKan(): Boolean {
        return type == Type.KAN || type == Type.LATE_KAN || type == Type.CONCEALED_KAN
    }

    companion object {
        fun List<Call>.excludeConcealedKan(): List<Call> {
            return this.filter { it.type != Type.CONCEALED_KAN }
        }
    }
}