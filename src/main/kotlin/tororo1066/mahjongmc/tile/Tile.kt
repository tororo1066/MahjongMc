package tororo1066.mahjongmc.tile

import tororo1066.mahjongmc.enums.PlayerSettings
import java.util.UUID

class Tile(
    var type: TileType,
    var number: Int = 0,
    var honor: HonorTiles = HonorTiles.UNDEFINED,
    var isRed: Boolean = false
) {

    var isRichiTile: Boolean = false
    var isReplacementTile: Boolean = false
    var isSelfKanTile: Boolean = false

    val uuid: UUID = UUID.randomUUID()

    constructor(): this(TileType.HONORS, 0)

    companion object {
        fun fromIndex(index: Int): Tile {
            val type = TileType.entries[index / 9]
            return if (type == TileType.HONORS) {
                Tile(
                    type = type,
                    honor = HonorTiles.entries[index % 9]
                )
            } else {
                Tile(
                    type = type,
                    number = (index % 9) + 1
                )
            }
        }
    }

    fun parameters(): MutableMap<String, Any> {
        return mutableMapOf(
            "type" to type.name,
            "number" to number,
            "honor" to honor.name,
            "isRed" to isRed.toString(),
            "uuid" to uuid.toString(),
            "customModelData" to getCustomModelData()
        )
    }

    // 0-33までのインデックスを取得
    fun getIndex(): Int {
        val base = type.ordinal * 9
        return if (type == TileType.HONORS) {
            base + honor.ordinal
        } else {
            base + number - 1
        }
    }

    fun getCustomModelData(): Int {
        val base = type.ordinal * 10
        return if (type == TileType.HONORS) {
            base + honor.ordinal
        } else {
            if (isRed) {
                base + 9
            } else {
                base + number - 1
            }
        }
    }

    fun isTermsOrHonors(): Boolean {
        return type == TileType.HONORS || isTerminal()
    }

    fun isTerminal(): Boolean {
        return number == 1 || number == 9
    }

    // 他のタイルと同じかどうかを判定
    // 赤ドラは区別しない
    fun isSameTile(other: Tile): Boolean {
        return this.type == other.type &&
                this.number == other.number &&
                this.honor == other.honor
    }

    fun getBonusTile(playerSettings: PlayerSettings = PlayerSettings.PLAYERS_4): Tile {
        when (type) {
            TileType.CHARACTERS, TileType.DOTS, TileType.BAMBOO -> {
                return if (type == TileType.CHARACTERS && playerSettings == PlayerSettings.PLAYERS_3) {
                    // 3人麻雀の場合2から8は使用しない
                    if (number == 9) Tile(type, 1) else Tile(type, 9)
                } else {
                    Tile(type, (number + 1) % 9)
                }
            }
            TileType.HONORS -> {
                val type = if (honor.isDragon()) {
                    when (honor) {
                        HonorTiles.RED -> HonorTiles.GREEN
                        HonorTiles.GREEN -> HonorTiles.WHITE
                        HonorTiles.WHITE -> HonorTiles.RED
                        else -> honor
                    }
                } else {
                    when (honor) {
                        HonorTiles.EAST -> HonorTiles.SOUTH
                        HonorTiles.SOUTH -> HonorTiles.WEST
                        HonorTiles.WEST -> if (playerSettings == PlayerSettings.PLAYERS_3) {
                            HonorTiles.EAST
                        } else {
                            HonorTiles.NORTH
                        }
                        HonorTiles.NORTH -> HonorTiles.EAST
                        else -> honor
                    }
                }

                return Tile(TileType.HONORS, honor = type)
            }
        }
    }

    fun copyTileInfo(tile: Tile): Tile {
        this.type = tile.type
        this.number = tile.number
        this.honor = tile.honor
        this.isRed = tile.isRed
        return this
    }

    override fun toString(): String {
        return "Tile(type=$type, number=$number, honor=$honor, isRed=$isRed, uuid=$uuid)"
    }
}