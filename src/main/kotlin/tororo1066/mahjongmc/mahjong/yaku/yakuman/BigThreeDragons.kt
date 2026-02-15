package tororo1066.mahjongmc.mahjong.yaku.yakuman

import tororo1066.mahjongmc.enums.Position
import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.Meld
import tororo1066.mahjongmc.mahjong.WinningStructure
import tororo1066.mahjongmc.tile.HonorTiles

object BigThreeDragons: AbstractYakumanResponsiblePaymentWinning() {
    override val name: String = "大三元"

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        val triplets = winningStructure.winningTiles.melds.filter {
            it is Meld.Triplet || it is Meld.Quadruplet
        }
        return triplets.any { it.tiles.first().honor == HonorTiles.WHITE }
                && triplets.any { it.tiles.first().honor == HonorTiles.GREEN }
                && triplets.any { it.tiles.first().honor == HonorTiles.RED }
    }

    // 責任払いの判定
    override fun checkPao(player: PlayerInstance): Position? {
        val calls = player.calls
        val dragonCalls = calls.filter { call ->
            call.tiles.first().honor.isDragon()
        }

        if (dragonCalls.size != 3) return null
        return dragonCalls.last().target
    }
}