package tororo1066.mahjongmc.mahjong.yaku.three

import tororo1066.mahjongmc.Call.Companion.excludeConcealedKan
import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.Meld
import tororo1066.mahjongmc.mahjong.WinningStructure
import tororo1066.mahjongmc.tile.TileType

object TwoEvenElements: AbstractThreeWinning() {
    override val name: String = "二盃口"

    private data class SequenceInfo(val type: TileType, val start: Int)

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        // 副露がある時点でアウト
        if (player.calls.excludeConcealedKan().isNotEmpty()) return false

        val melds = winningStructure.winningTiles.melds

        // 4面子すべてが順子で、同じ順子が2組ある
        if (!melds.all { it is Meld.Sequence }) return false

        val countMap = mutableMapOf<SequenceInfo, Int>()

        for (meld in melds) {
            val first = meld.tiles.first()
            val info = SequenceInfo(first.type, first.number)
            countMap[info] = countMap.getOrDefault(info, 0) + 1
        }

        return countMap.values.count { it == 2 } == 2
    }
}