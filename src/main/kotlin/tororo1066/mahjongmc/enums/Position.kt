package tororo1066.mahjongmc.enums

enum class Position(val yaw: Float, val displayName: String) {
    EAST(-90f, "東"),
    SOUTH(0f, "南"),
    WEST(90f, "西"),
    NORTH(-180f, "北"),
    ;

    fun getMinecraftYaw(dealerPosition: Position, playerSettings: PlayerSettings): Float {
        val seats = playerSettings.seats
        val index = (this.ordinal - dealerPosition.ordinal + seats) % seats
        return entries[index].yaw
    }

    fun getRadians(dealerPosition: Position, playerSettings: PlayerSettings): Double {
        // MinecraftのYawは時計回りが正なので符号を反転
        return Math.toRadians(-getMinecraftYaw(dealerPosition, playerSettings).toDouble())
    }


    fun next(playerSettings: PlayerSettings): Position {
        val seats = playerSettings.seats
        return entries[(ordinal + 1) % seats]
    }

    fun center(playerSettings: PlayerSettings): Position? {
        val seats = playerSettings.seats
        if (seats % 2 == 1) {
            return null
        }
        return entries[(ordinal + seats / 2) % seats]
    }

    fun previous(playerSettings: PlayerSettings): Position {
        val seats = playerSettings.seats
        return entries[(ordinal + seats - 1) % seats]
    }
}