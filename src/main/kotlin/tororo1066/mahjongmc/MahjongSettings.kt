package tororo1066.mahjongmc

import tororo1066.mahjongmc.enums.PlayerSettings

data class MahjongSettings(
    val playerSettings: PlayerSettings = PlayerSettings.PLAYERS_4,
    val initialScore: Int = 25000,
    val endScore: Int = 30000,
    val allowKuigae: Boolean = false,
    val battleType: BattleType = BattleType.EAST_ONLY
) {

    enum class BattleType(
        val roundsOfWindChange: Int
    ) {
        EAST_ONLY(0),
        EAST_SOUTH(1),
        FULL(3);
    }
}