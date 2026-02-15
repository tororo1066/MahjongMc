package tororo1066.mahjongmc.mahjong.yaku.one

import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.tile.TileType
import tororo1066.mahjongmc.mahjong.WinningStructure

object AllSimples: AbstractOneWinning() {

    override val name: String = "断幺九"

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        return winningStructure.winningTiles.tiles.all { tile ->
            tile.type != TileType.HONORS && tile.number in 2..8
        }
    }
}