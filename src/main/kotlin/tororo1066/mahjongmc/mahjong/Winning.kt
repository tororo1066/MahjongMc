package tororo1066.mahjongmc.mahjong

data class Winning(
    val winnings: List<AbstractWinning>,
    val winningStructure: WinningStructure,
    val baseScore: Int
)