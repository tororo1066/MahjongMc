package tororo1066.mahjongmc.mahjong.yaku.yakuman

import tororo1066.mahjongmc.enums.Position
import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.Meld
import tororo1066.mahjongmc.mahjong.WinningStructure
import tororo1066.mahjongmc.tile.HonorTiles

object BigFourWinds: AbstractYakumanResponsiblePaymentWinning() {
    override val name: String = "大四喜"

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        val triplets = winningStructure.winningTiles.melds.filter {
            it is Meld.Triplet || it is Meld.Quadruplet
        }

        return triplets.any { it.tiles.first().honor == HonorTiles.EAST } &&
                triplets.any { it.tiles.first().honor == HonorTiles.SOUTH } &&
                triplets.any { it.tiles.first().honor == HonorTiles.WEST } &&
                triplets.any { it.tiles.first().honor == HonorTiles.NORTH }
    }

    override fun checkPao(player: PlayerInstance): Position? {
        val calls = player.calls
        val windCalls = calls.filter { call ->
            call.tiles.first().honor.isWind()
        }

        if (windCalls.size != 4) return null
        return windCalls.last().target
    }
}