package tororo1066.mahjongmc.game.ui

object MahjongDisplays {
    const val TABLE_DISPLAY = "table_display"
    const val STATE_DISPLAY = "state_display"
    const val POSITION_DISPLAY = "position_display"
    const val TILE_DISPLAY = "tile_display"
    const val TIME_DISPLAY = "display_time"
    const val WINNING_DISPLAY = "display_winning"
    const val ACTION_DISPLAY = "display_action"
    const val SKIP_DISPLAY = "${ACTION_DISPLAY}_skip"
    const val CHOICE_DISPLAY = "${ACTION_DISPLAY}_choice"
    const val CHOICE_CANCEL_DISPLAY = "${CHOICE_DISPLAY}_cancel"

    const val PON_DISPLAY = "${ACTION_DISPLAY}_pon"
    const val CHI_DISPLAY = "${ACTION_DISPLAY}_chi"
    const val KAN_DISPLAY = "${ACTION_DISPLAY}_kan"
    const val RON_DISPLAY = "${ACTION_DISPLAY}_ron"
    const val RICHI_DISPLAY = "${ACTION_DISPLAY}_richi"
    const val RICHI_CANCEL_DISPLAY = "${RICHI_DISPLAY}_cancel"
    const val TSUMO_DISPLAY = "${ACTION_DISPLAY}_tsumo"
    const val RYUKYOKU_ACTION_DISPLAY = "${ACTION_DISPLAY}_ryukyoku"

    const val SCORE_CHANGE_DISPLAY = "display_score_change"
    const val RYUKYOKU_DISPLAY = "display_ryukyoku"

    fun String.appendSuffix(suffix: String): String {
        return this + "_" + suffix
    }
}