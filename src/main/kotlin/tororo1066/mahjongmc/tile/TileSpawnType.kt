package tororo1066.mahjongmc.tile

enum class TileSpawnType {
    HAND,
    DEAD_WALL,
    CHOICE,
    WINNING,
    TENPAI;

    fun lowercase(): String {
        return this.name.lowercase()
    }
}