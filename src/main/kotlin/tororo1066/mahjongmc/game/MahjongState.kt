package tororo1066.mahjongmc.game

import tororo1066.mahjongmc.DeadWall
import tororo1066.mahjongmc.enums.Position
import tororo1066.mahjongmc.tile.Tile

class MahjongState {
    // プレイヤーリスト
    val players = mutableListOf<PlayerInstance>()
    // 牌リスト
    val generalTiles = mutableListOf<Tile>()
    // 牌山
    val deadWall = DeadWall()

    var turn: Position = Position.EAST
    lateinit var turnPlayer: PlayerInstance
    var turnDirty: Boolean = false

    var richiSticks: Int = 0

    var round: Int = 1
    var roundWind: Position = Position.EAST
    var dealerPosition: Position = Position.EAST
}