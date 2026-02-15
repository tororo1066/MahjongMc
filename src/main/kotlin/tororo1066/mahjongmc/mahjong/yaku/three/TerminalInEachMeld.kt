package tororo1066.mahjongmc.mahjong.yaku.three

import tororo1066.mahjongmc.Call.Companion.excludeConcealedKan
import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.WinningStructure
import tororo1066.mahjongmc.tile.Tile

object TerminalInEachMeld: AbstractThreeWinning() {
    override val name: String = "純全帯么九"

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        fun check(tiles: List<Tile>): Boolean {
            return tiles.any { it.isTerminal() }
        }

        return winningStructure.winningTiles.melds.all { meld ->
            check(meld.tiles)
        } && check(winningStructure.winningTiles.heads)
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