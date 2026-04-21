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
        return HonorTiles.DRAGONS.all { wind ->
            winningStructure.winningTiles.melds.any { meld ->
                meld.tiles.all { it.honor == wind }
            }
        }
    }

    // 責任払いの判定
    override fun checkTarget(player: PlayerInstance): Position? {
        val calls = player.calls
        val dragonCalls = calls.filter { call ->
            call.tiles.first().honor.isDragon()
        }

        if (dragonCalls.size != 3) return null
        val target = dragonCalls.last().target
        if (player.position == target) return null
        return target
    }
}