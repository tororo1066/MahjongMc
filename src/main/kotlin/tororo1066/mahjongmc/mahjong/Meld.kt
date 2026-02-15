package tororo1066.mahjongmc.mahjong

import tororo1066.mahjongmc.Call
import tororo1066.mahjongmc.tile.Tile

sealed class Meld {
    abstract val tiles: List<Tile>
    abstract val isCalled: Boolean
    data class Sequence(override val tiles: List<Tile>, override val isCalled: Boolean = false): Meld()
    data class Triplet(override val tiles: List<Tile>, override val isCalled: Boolean = false): Meld()
    data class Quadruplet(override val tiles: List<Tile>, override val isCalled: Boolean = false): Meld()
    // 七対子用
    data class Pair(override val tiles: List<Tile>): Meld() {
        override val isCalled: Boolean = false
    }
    // 国士無双用
    data class Single(val tile: Tile): Meld() {
        override val tiles: List<Tile> = listOf(tile)
        override val isCalled: Boolean = false
    }


    companion object {
        fun byCall(call: Call): Meld {
            return when (call.type) {
                Call.Type.PON -> Triplet(call.tiles, true)
                Call.Type.CHI -> Sequence(call.tiles.sortedBy { it.getIndex() }, true)
                Call.Type.KAN, Call.Type.LATE_KAN, Call.Type.CONCEALED_KAN -> Quadruplet(call.tiles, true)
            }
        }
    }
}