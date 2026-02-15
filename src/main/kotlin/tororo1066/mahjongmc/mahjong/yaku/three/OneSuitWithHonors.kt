package tororo1066.mahjongmc.mahjong.yaku.three

import tororo1066.mahjongmc.Call.Companion.excludeConcealedKan
import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.WinningStructure
import tororo1066.mahjongmc.tile.TileType

object OneSuitWithHonors: AbstractThreeWinning() {
    override val name: String = "混一色"

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        val firstTile = winningStructure.winningTiles.tiles.firstOrNull {
            it.type != TileType.HONORS
        } ?: return false

        return winningStructure.winningTiles.tiles.all {
            it.type == firstTile.type || it.type == TileType.HONORS
        }
    }

    override fun getHan(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Int {
        // 門前であれば3翻、鳴いていれば2翻
        return if (player.calls.excludeConcealedKan().isEmpty()) 3 else 2
    }
}