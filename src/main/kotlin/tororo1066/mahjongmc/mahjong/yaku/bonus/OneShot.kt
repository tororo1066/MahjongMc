package tororo1066.mahjongmc.mahjong.yaku.bonus

import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.WinningStructure
import tororo1066.mahjongmc.mahjong.yaku.one.AbstractOneWinning

object OneShot: AbstractOneWinning() {
    override val name: String = "一発"
    override val hasYaku: Boolean = false

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        return player.isOneShot
    }
}