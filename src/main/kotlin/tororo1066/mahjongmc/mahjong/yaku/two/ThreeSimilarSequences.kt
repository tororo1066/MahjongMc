package tororo1066.mahjongmc.mahjong.yaku.two

import tororo1066.mahjongmc.Call.Companion.excludeConcealedKan
import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.Meld
import tororo1066.mahjongmc.mahjong.WinningStructure
import tororo1066.mahjongmc.tile.TileType

object ThreeSimilarSequences: AbstractTwoWinning() {
    override val name: String = "三色同順"

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        val sequences = winningStructure.winningTiles.melds.filterIsInstance<Meld.Sequence>()

        val map = mutableMapOf<Int, MutableSet<TileType>>()

        for (sequence in sequences) {
            val first = sequence.tiles.first()
            val type = first.type
            if (type == TileType.HONORS) continue

            val number = first.number
            map.computeIfAbsent(number) { mutableSetOf() }.add(type)
        }

        return map.values.any { types ->
            types.containsAll(setOf(TileType.CHARACTERS, TileType.DOTS, TileType.BAMBOO))
        }
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