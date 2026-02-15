package tororo1066.mahjongmc.mahjong.yaku.one

import tororo1066.mahjongmc.Call.Companion.excludeConcealedKan
import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.Meld
import tororo1066.mahjongmc.mahjong.WinningStructure

object TwinSequences: AbstractOneWinning() {
    override val name: String = "一盃口"

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        if (player.calls.excludeConcealedKan().isNotEmpty()) return false
        val sequences = winningStructure.winningTiles.melds.filterIsInstance<Meld.Sequence>()

        val counted = mutableSetOf<Int>()
        for (index in sequences.indices) {
            if (index in counted) continue
            val first = sequences[index]
            for (j in index + 1 until sequences.size) {
                if (j in counted) continue
                val second = sequences[j]
                for (tileIndex in first.tiles.indices) {
                    if (!first.tiles[tileIndex].isSameTile(second.tiles[tileIndex])) {
                        return false
                    }
                }
            }
        }
        return true
    }
}