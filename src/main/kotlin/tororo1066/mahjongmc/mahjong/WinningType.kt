package tororo1066.mahjongmc.mahjong

enum class WinningType(val displayName: String) {
    NORMAL("通常"),
    MANGAN("満貫"),
    HANEMAN("跳満"),
    BAIMAN("倍満"),
    SANBAIMAN("三倍満"),
    YAKUMAN("役満"),
    DOUBLE_YAKUMAN("ダブル役満"),
    TRIPLE_YAKUMAN("トリプル役満"),
    COUNTED_YAKUMAN("数え役満")
}