package tororo1066.mahjongmc.api

import tororo1066.mahjongmc.enums.Position
import java.util.UUID

interface PlayerInstanceView {
    val uuid: UUID
    val name: String
    val score: Int
    val holdTimes: Int
    val backupTimes: Int

    val isRichi: Boolean
    val isDoubleRichi: Boolean
    val isOneShot: Boolean
    val position: Position

    val tiles: List<TileView>
}