package tororo1066.mahjongmc.mahjong.yaku.other

import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.AbstractWinning
import tororo1066.mahjongmc.mahjong.WinningStructure

object DiscardTerminalsAndHonorsThroughout: AbstractWinning() {
    override val name: String = "流し満貫"

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        return false // 判定方法が特別なため、ここでは常にfalseを返す。実際の判定はゲームのロジック内で行う。
//        return player.discard.all { it.type == TileType.HONORS || it.number == 1 || it.number == 9 }
//                && instance.players.none { it.calls.any { call -> call.target == player.position } }
    }

    override fun getHan(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Int {
        return 0
    }
}