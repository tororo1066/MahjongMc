package tororo1066.mahjongmc.mahjong.yaku.yakuman

import tororo1066.mahjongmc.enums.Position
import tororo1066.mahjongmc.game.PlayerInstance

abstract class AbstractYakumanResponsiblePaymentWinning: AbstractYakumanWinning() {
    abstract fun checkPao(player: PlayerInstance): Position?
}