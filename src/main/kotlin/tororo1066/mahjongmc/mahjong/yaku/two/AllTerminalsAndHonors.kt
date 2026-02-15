package tororo1066.mahjongmc.mahjong.yaku.two

import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.WinningStructure

object AllTerminalsAndHonors: AbstractTwoWinning() {
    override val name: String = "混老頭"

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        return winningStructure.winningTiles.tiles.all { tile ->
            tile.isTermsOrHonors()
        }
    }
}