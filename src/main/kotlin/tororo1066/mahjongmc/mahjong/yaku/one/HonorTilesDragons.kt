package tororo1066.mahjongmc.mahjong.yaku.one

import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.mahjong.WinningStructure

object HonorTilesDragons: AbstractOneWinning() {
    override val name: String = "役牌:三元牌"

    override fun check(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): Boolean {
        return winningStructure.winningTiles.melds.any { meld ->
            meld.tiles.all { tile ->
                tile.honor.isDragon()
            }
        }
    }
}