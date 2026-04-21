package tororo1066.mahjongmc.game

import tororo1066.mahjongmc.tile.Tile

sealed class RyukyokuType {
    class Normal(val tenpaiTiles: Map<PlayerInstance, List<Tile>>): RyukyokuType()
    class NineDifferentTerminals(val player: PlayerInstance): RyukyokuType()
    object FourPlayersRichi: RyukyokuType()
    object DiscardingTheSameWind: RyukyokuType()
    object MeldedFourFours: RyukyokuType()
}