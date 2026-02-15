package tororo1066.mahjongmc.mahjong.yaku.one

import tororo1066.mahjongmc.Call.Companion.excludeConcealedKan
import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.tile.TileType
import tororo1066.mahjongmc.mahjong.WaitType
import tororo1066.mahjongmc.mahjong.Meld
import tororo1066.mahjongmc.mahjong.WinningStructure

object Pinfu: AbstractOneWinning() {
    override val name: String = "平和"

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        // 副露がある時点でアウト
        if (player.calls.excludeConcealedKan().isNotEmpty()) return false
        // 面子が全て順子であることを確認
        if (winningStructure.winningTiles.melds.any { meld -> meld is Meld.Sequence }) return false
        // 雀頭が役牌・場風・自風でないことを確認
        val head = winningStructure.winningTiles.heads.first()
        if (head.type == TileType.HONORS) {
            if (head.honor.isDragon()) return false
            val position = head.honor.getPosition() ?: return false
            if (position == instance.roundWind || position == player.position) return false
        }
        // 待ちが両面待ちであることを確認
        return winningStructure.getWaitType() == WaitType.RYANMEN
    }
}