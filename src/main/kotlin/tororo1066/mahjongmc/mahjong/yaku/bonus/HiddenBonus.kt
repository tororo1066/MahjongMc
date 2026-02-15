package tororo1066.mahjongmc.mahjong.yaku.bonus

import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.AbstractWinning
import tororo1066.mahjongmc.mahjong.WinningStructure

object HiddenBonus: AbstractWinning() {
    override val name: String = "裏ドラ"
    override val hasYaku: Boolean = false

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        if (!player.isRichi) return false
        return player.allHandTiles.any { tile ->
            instance.deadWall.isHiddenBonusTile(tile)
        }
    }

    override fun getHan(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Int {
        if (!player.isRichi) return 0
        return player.allHandTiles.count { tile ->
            instance.deadWall.isHiddenBonusTile(tile)
        }
    }
}