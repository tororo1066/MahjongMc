package tororo1066.mahjongmc.mahjong.yaku.two

import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.Meld
import tororo1066.mahjongmc.mahjong.WinningStructure

object LittleThreeDragons: AbstractTwoWinning() {
    override val name: String = "小三元"

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        val melds = winningStructure.winningTiles.melds

        // 三元牌の刻子/槓子を数える（槓子も刻子相当として扱う）
        val dragonTripletOrQuadCount = melds.count { meld ->
            when (meld) {
                is Meld.Triplet, is Meld.Quadruplet -> meld.tiles.all { it.honor.isDragon() }
                else -> false
            }
        }

        if (dragonTripletOrQuadCount != 2) return false

        // 雀頭が三元牌
        val head = winningStructure.winningTiles.heads.firstOrNull() ?: return false
        return head.honor.isDragon()
    }
}