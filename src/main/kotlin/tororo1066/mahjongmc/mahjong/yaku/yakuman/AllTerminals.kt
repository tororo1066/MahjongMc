package tororo1066.mahjongmc.mahjong.yaku.yakuman

import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.WinningStructure

object AllTerminals: AbstractYakumanWinning() {
    override val name: String = "清老頭"

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        return winningStructure.winningTiles.tiles.all { it.isTerminal() }
    }
}