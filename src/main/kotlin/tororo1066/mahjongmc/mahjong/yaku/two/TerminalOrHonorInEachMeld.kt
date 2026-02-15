package tororo1066.mahjongmc.mahjong.yaku.two

import tororo1066.mahjongmc.Call.Companion.excludeConcealedKan
import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.WinningStructure
import tororo1066.mahjongmc.tile.Tile

object TerminalOrHonorInEachMeld: AbstractTwoWinning() {
    override val name: String = "混全帯么九"

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        fun check(tiles: List<Tile>): Boolean {
            return tiles.any { it.isTermsOrHonors() }
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
        // 門前であれば2翻、鳴いていれば1翻
        return if (player.calls.excludeConcealedKan().isEmpty()) 2 else 1
    }
}