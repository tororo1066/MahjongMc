package tororo1066.mahjongmc.mahjong.yaku.two

import tororo1066.mahjongmc.Call.Companion.excludeConcealedKan
import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.Meld
import tororo1066.mahjongmc.mahjong.WinningStructure
import tororo1066.mahjongmc.tile.TileType

object ThreeConsecutiveSequences: AbstractTwoWinning() {
    override val name: String = "一気通貫"

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        // 一気通貫: 同一色で 123 / 456 / 789 の順子が揃う
        // ＝ 同一色の順子の開始数字が {1,4,7} を全て含む
        val startNumbersByType: Map<TileType, Set<Int>> = winningStructure.winningTiles.melds
            .asSequence()
            .filterIsInstance<Meld.Sequence>()
            .groupBy { it.tiles.first().type }
            .filterKeys { it != TileType.HONORS }
            .mapValues { (_, sequences) -> sequences.map { it.tiles.first().number }.toSet() }

        return startNumbersByType.values.any { starts ->
            starts.containsAll(setOf(1, 4, 7))
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