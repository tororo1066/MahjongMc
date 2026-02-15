package tororo1066.mahjongmc.tile

import tororo1066.mahjongmc.enums.Position

enum class HonorTiles {
    EAST,
    SOUTH,
    WEST,
    NORTH,
    WHITE,
    GREEN,
    RED,
    UNDEFINED;

    fun getPosition(): Position? {
        return when(this) {
            EAST -> Position.EAST
            SOUTH -> Position.SOUTH
            WEST -> Position.WEST
            NORTH -> Position.NORTH
            else -> null
        }
    }

    fun isDragon(): Boolean {
        return this == WHITE || this == GREEN || this == RED
    }

    fun isWind(): Boolean {
        return this == EAST || this == SOUTH || this == WEST || this == NORTH
    }
}