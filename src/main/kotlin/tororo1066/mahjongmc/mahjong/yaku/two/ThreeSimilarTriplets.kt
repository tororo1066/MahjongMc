package tororo1066.mahjongmc.mahjong.yaku.two

import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.Meld
import tororo1066.mahjongmc.mahjong.WinningStructure
import tororo1066.mahjongmc.tile.TileType

object ThreeSimilarTriplets: AbstractTwoWinning() {
    override val name: String = "三色同刻"

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        val triplets = winningStructure.winningTiles.melds.filter {
            it is Meld.Triplet || it is Meld.Quadruplet
        }
        if (triplets.size < 3) return false

        // 刻子/槓子の種類を数える
        val map = mutableMapOf<Int, MutableSet<TileType>>()
        for (meld in triplets) {
            val first = meld.tiles.first()
            val type = first.type
            if (type == TileType.HONORS) continue // 字牌は無視
            val number = first.number
            map.computeIfAbsent(number) { mutableSetOf() }.add(type)
        }

        // 同じ数牌の刻子/槓子が3種類以上あれば成立
        return map.values.any { types ->
            types.containsAll(setOf(TileType.CHARACTERS, TileType.DOTS, TileType.BAMBOO))
        }
    }
}