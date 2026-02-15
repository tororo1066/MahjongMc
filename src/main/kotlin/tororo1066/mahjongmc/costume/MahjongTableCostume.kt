package tororo1066.mahjongmc.costume

import org.bukkit.Location
import org.bukkit.util.Vector
import tororo1066.displaymonitorapi.configuration.IAdvancedConfigurationSection
import tororo1066.mahjongmc.Call
import tororo1066.mahjongmc.Call.Type
import tororo1066.mahjongmc.MahjongMc
import tororo1066.mahjongmc.Util.get3Point
import tororo1066.mahjongmc.Util.get3PointList
import tororo1066.mahjongmc.Util.nullIfFalse
import tororo1066.mahjongmc.enums.PlayerSettings
import tororo1066.mahjongmc.enums.Position
import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.tile.Tile
import tororo1066.tororopluginapi.SJavaPlugin
import tororo1066.tororopluginapi.utils.setPitchL
import tororo1066.tororopluginapi.utils.setYawL
import java.io.File

class MahjongTableCostume: AbstractCostume() {

    lateinit var playerSettings: PlayerSettings

    override fun init(instance: MahjongInstance) {
        playerSettings = instance.settings.playerSettings
    }

    private fun Position.getRadians(dealerPosition: Position): Double {
        return this.getRadians(dealerPosition, playerSettings)
    }

    private fun Position.getMinecraftYaw(dealerPosition: Position): Float {
        return this.getMinecraftYaw(dealerPosition, playerSettings)
    }

    fun getOnSpawn(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("onSpawn")
        } ?: emptyList()
    }

    fun getPlacementLocation(
        path: String,
        center: Location,
        index: Int,
        position: Position,
        dealerPosition: Position
    ): Location {
        val fixedOffset = getOrDefault { it.get3Point("$path.offset.fixed") } ?: Vector(0, 0, 0)
        val offsetList = getOrDefault {
            it.isList("$path.offset.list").nullIfFalse {
                it.get3PointList("$path.offset.list")
            }
        }
        val simpleOffset = getOrDefault { it.get3Point("$path.offset.simple") } ?: Vector(0, 0, 0)

        val rotation = position.getRadians(dealerPosition)

        val location = center.clone().add(fixedOffset.rotateAroundY(rotation))

        if (offsetList != null) {
            offsetList.getOrNull(index)?.let { location.add(it.rotateAroundY(rotation)) }
        } else {
            location.add(simpleOffset.clone().multiply(index.toDouble()).rotateAroundY(rotation))
        }

        return location.setYawL(position.getMinecraftYaw(dealerPosition))
    }

    fun getTranslation(
        path: String,
        index: Int,
        position: Position,
        dealerPosition: Position
    ): Vector {
        val fixedOffset = getOrDefault { it.get3Point("$path.translation.fixed") } ?: Vector(0, 0, 0)
        val offsetList = getOrDefault {
            it.isList("$path.translation.list").nullIfFalse {
                it.get3PointList("$path.translation.list")
            }
        }
        val simpleOffset = getOrDefault { it.get3Point("$path.translation.simple") } ?: Vector(0, 0, 0)

        val rotation = position.getRadians(dealerPosition)

        val vector = fixedOffset.rotateAroundY(rotation)

        if (offsetList != null) {
            offsetList.getOrNull(index)?.let {
                return vector.add(it.rotateAroundY(rotation))
            }
        } else {
            return vector.add(simpleOffset.clone().multiply(index.toDouble()).rotateAroundY(rotation))
        }

        return vector
    }

    fun getTileLocation(center: Location, tileIndex: Int, position: Position, dealerPosition: Position, lastTile: Boolean = false): Location {
//        return getPlacementLocation("tile", center, tileIndex, position, dealerPosition)
        val base = getPlacementLocation("tile", center, tileIndex, position, dealerPosition)

        val lastTileOffset = if (lastTile) {
            getOrDefault { it.get3Point("tile.offset.last") } ?: Vector(0, 0, 0)
        } else Vector(0, 0, 0)

        val rotation = position.getRadians(dealerPosition)
        return base.add(lastTileOffset.rotateAroundY(rotation))
    }

    fun getDiscardLocation(center: Location, discardIndex: Int, position: Position, dealerPosition: Position): Location {
        val fixedOffset = getOrDefault { it.get3Point("discard.offset.fixed") } ?: Vector(0, 0, 0)
        val discardOffset = getOrDefault {
            it.isList("discard.offset.list").nullIfFalse {
                it.get3PointList("discard.offset.list")
            }
        }
        val simpleDiscardOffset = getOrDefault { it.get3Point("discard.offset.simple") } ?: Vector(0, 0, 0)
        val discardSplit = getOrDefault { it.get3Point("discard.offset.split") } ?: Vector(0, 0, 0)

        val rotation = position.getRadians(dealerPosition)

        val location = center.clone().add(fixedOffset.rotateAroundY(rotation))

        if (discardOffset != null) {
            discardOffset.getOrNull(discardIndex)?.let { location.add(it.rotateAroundY(rotation)) }
        } else {
            val split = getDiscardSplit()
            if (split.isNotEmpty()) {
                val row = split.sorted().indexOfFirst { discardIndex < it }.let { if (it == -1) split.size else it }
                val index = if (row == 0) discardIndex else discardIndex - split[row - 1]
                location.add(simpleDiscardOffset.clone().multiply(index.toDouble()).rotateAroundY(rotation))
                location.add(discardSplit.clone().multiply(row.toDouble()).rotateAroundY(rotation))
            } else {
                location.add(simpleDiscardOffset.clone().multiply(discardIndex.toDouble()).rotateAroundY(rotation))
            }
        }

        return location.setYawL(position.getMinecraftYaw(dealerPosition)).setPitchL(-90f)
    }

    fun getDiscardRichiOffset(): Vector {
        return getOrDefault { it.get3Point("discard.offset.richi.offset") } ?: Vector(0, 0, 0)
    }

    fun getDiscardSplit(): List<Int> {
        return getOrDefault {
            it.isList("discard.split").nullIfFalse {
                it.getIntegerList("discard.split")
            }
        } ?: emptyList()
    }

    fun getOnDiscard(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("onDiscard")
        } ?: emptyList()
    }

    fun getDeadWallTileLocation(
        center: Location,
        tileIndex: Int,
        position: Position,
        dealerPosition: Position,
        opened: Boolean
    ): Location {
        val fixedOffset = getOrDefault { it.get3Point("deadwall.offset.fixed") }  ?: Vector(0, 0, 0)
        val tileOffset = getOrDefault {
            it.isList("deadwall.offset.list").nullIfFalse {
                it.get3PointList("deadwall.offset.list")
            }
        }
        val simpleVerticalTileOffset = getOrDefault { it.get3Point("deadwall.offset.simple.vertical") }  ?: Vector(0, 0, 0)
        val simpleHorizontalTileOffset = getOrDefault { it.get3Point("deadwall.offset.simple.horizontal") }  ?: Vector(0, 0, 0)

        val rotation = position.getRadians(dealerPosition)
        val location = center.clone().add(fixedOffset.rotateAroundY(rotation))
        if (tileOffset != null) {
            tileOffset.getOrNull(tileIndex)?.let { location.add(it.rotateAroundY(rotation)) }
        } else {
            val addVertical = tileIndex >= 5
            if (addVertical) {
                location.add(simpleVerticalTileOffset.clone().rotateAroundY(rotation))
            }

            val index = if (addVertical) tileIndex - 5 else tileIndex
            location.add(simpleHorizontalTileOffset.clone().multiply(index.toDouble()).rotateAroundY(rotation))
        }

        return location.setYawL(position.getMinecraftYaw(dealerPosition)).setPitchL(if (opened) -90f else 90f)
    }

    fun getOnStateChange(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("onStateChange")
        } ?: emptyList()
    }

    fun getOnRenderPosition(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("onRenderPosition")
        } ?: emptyList()
    }

    fun getActionDisplayPosition(center: Location, index: Int, position: Position, dealerPosition: Position): Location {
        val fixed = getOrDefault { it.get3Point("action.display.position.fixed") }  ?: Vector(0, 0, 0)
        val list = getOrDefault  {
            it.isList("action.display.position.list").nullIfFalse {
                it.get3PointList("action.display.position.list")
            }
        }
        val simpleOffset = getOrDefault { it.get3Point("action.display.position.offset") }  ?: Vector(0, 0, 0)

        val rotation = position.getRadians(dealerPosition)

        val location = center.clone().add(fixed.rotateAroundY(rotation))
        if (list != null) {
            location.add((list.getOrNull(index) ?: Vector(0, 0, 0)).rotateAroundY(rotation))
        } else {
            location.add(simpleOffset.clone().multiply(index.toDouble()).rotateAroundY(rotation))
        }
        return location.setYawL(position.getMinecraftYaw(dealerPosition))
    }

    fun getSkipDisplay(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("action.display.skip")
        } ?: emptyList()
    }

    fun getPonDisplay(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("action.display.pon")
        } ?: emptyList()
    }

    fun getChiDisplay(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("action.display.chi")
        } ?: emptyList()
    }

    fun getKanDisplay(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("action.display.kan")
        } ?: emptyList()
    }

    fun getRichiDisplay(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("action.display.richi")
        } ?: emptyList()
    }

    fun getCancelDisplay(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("action.display.cancel")
        } ?: emptyList()
    }

    fun getTsumoDisplay(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("action.display.tsumo")
        } ?: emptyList()
    }

    fun getRonDisplay(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("action.display.ron")
        } ?: emptyList()
    }

    fun getChoicesDisplay(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("action.display.choices.actions")
        } ?: emptyList()
    }

    fun getChoicesCenterLocation(center: Location, position: Position, dealerPosition: Position): Location {
        val centerOffset = getOrDefault {
            it.get3Point("action.display.choices.center")
        } ?: Vector(0.0, 0.0, 0.0)

        val rotation = position.getRadians(dealerPosition)

        return center.clone().add(centerOffset.rotateAroundY(rotation)).setYawL(position.getMinecraftYaw(dealerPosition))
    }

    fun getChoicesTileLocations(
        center: Location,
        index: Int,
        size: Int,
        callType: Type,
        position: Position,
        dealerPosition: Position
    ): List<Location> {
        val tileWidth = getTileWidth()
        val centerLocation = getChoicesCenterLocation(center, position, dealerPosition)
        val distance = getOrDefault {
            it.get3Point("action.display.choices.distance")
        } ?: Vector(0.0, 0.0, 0.0)

        val rotation = position.getRadians(dealerPosition)

        val actualDistance = distance.clone().multiply((index - (size - 1) / 2.0)).rotateAroundY(rotation)
        val baseLocation = centerLocation.clone().add(actualDistance)

        val locations = mutableListOf<Location>()
        when (callType) {
            Type.PON, Type.CHI -> {
                repeat(2) { i ->
                    val location = baseLocation.clone().add(
                        Vector(-tileWidth / 2.0 + i * tileWidth, 0.0, 0.0).rotateAroundY(rotation)
                    )
                    locations.add(location.setYawL(position.getMinecraftYaw(dealerPosition)))
                }
            }
            Type.KAN -> {
                repeat(3) { i ->
                    val location = baseLocation.clone().add(
                        Vector(-tileWidth + i * tileWidth, 0.0, 0.0).rotateAroundY(rotation)
                    )
                    locations.add(location.setYawL(position.getMinecraftYaw(dealerPosition)))
                }
            }
            Type.LATE_KAN, Type.CONCEALED_KAN -> {
                repeat(4) { i ->
                    val location = baseLocation.clone().add(
                        Vector(-1.5 * tileWidth + i * tileWidth, 0.0, 0.0).rotateAroundY(rotation)
                    )
                    locations.add(location.setYawL(position.getMinecraftYaw(dealerPosition)))
                }
            }
        }

        return locations
    }

    fun getConcealDisplay(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("action.display.conceal")
        } ?: emptyList()
    }

    fun getRevealDisplay(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("action.display.reveal")
        } ?: emptyList()
    }

    fun getTileWidth(): Double {
        return getOrDefault {
            it.isDouble("tile.width").nullIfFalse {
                it.getDouble("tile.width")
            }
        } ?: 1.0
    }

    fun getTileHeight(): Double {
        return getOrDefault {
            it.isDouble("tile.height").nullIfFalse {
                it.getDouble("tile.height")
            }
        } ?: 1.0
    }

    private fun getCallPosition(instance: PlayerInstance, position: Int = -1): Vector {

        val base = getOrDefault {
            it.get3Point("action.call.position")
        } ?: Vector(0.0, 0.0, 0.0)

        val width = getTileWidth()
        val height = getTileHeight()

        fun vertical(): Vector {
            return Vector(width, 0.0, 0.0)
        }

        fun horizontal(): Vector {
            return Vector(height, 0.0, 0.0)
        }

        val vector = Vector(0.0, 0.0, 0.0)

        for (i in 0 until (if (position == -1) instance.calls.size else position)) {
            val call = instance.calls[i]
            when (call.type) {
                Type.PON, Type.CHI, Type.LATE_KAN -> {
                    vector.subtract(vertical().multiply(2.0))
                    vector.subtract(horizontal())
                }
                Type.KAN -> {
                    vector.subtract(vertical().multiply(3.0))
                    vector.subtract(horizontal())
                }
                Type.CONCEALED_KAN -> {
                    vector.subtract(vertical().multiply(4.0))
                }
            }
        }

        return base.add(vector)
    }

    fun getCallTileLocations(
        center: Location,
        call: Call,
        instance: PlayerInstance,
        position: Int = -1,
        dealerPosition: Position
    ): List<Pair<Tile, Location>> {
        val width = getTileWidth()
        val height = getTileHeight()

        val positions = mutableListOf<Pair<Tile, Location>>()
        val currentPosition = getCallPosition(instance, position)

        fun addPosition(
            tile: Tile,
            stepX: Double,
            offsetZ: Double,
            pitch: Float = -90f,
            yawAdjustment: Float = 0f
        ) {
            currentPosition.x -= stepX
            positions.add(
                tile to Location(
                    center.world,
                    currentPosition.x,
                    currentPosition.y,
                    currentPosition.z + offsetZ
                )
                    .setPitchL(pitch)
                    .setYawL(instance.position.getMinecraftYaw(dealerPosition) + yawAdjustment)
            )
            currentPosition.x -= stepX
        }

        fun addVertical(tile: Tile, pitch: Float = -90f) {
            addPosition(tile, width / 2, 0.0, pitch)
        }

        fun addHorizontal(tile: Tile) {
            addPosition(tile, height / 2, (height - width) / 2, yawAdjustment = -90f)
        }

        when (call.type) {
            Type.PON, Type.CHI -> {
                call.tiles.forEachIndexed { index, tile ->
                    val isSideways =
                        (index == 0 && instance.position.next(playerSettings) == call.target) ||
                        (index == 1 && instance.position.center(playerSettings) == call.target) ||
                        (index == 2 && instance.position.previous(playerSettings) == call.target)

                    if (isSideways) {
                        addHorizontal(tile)
                    } else {
                        addVertical(tile)
                    }
                }
            }
            Type.KAN -> {
                call.tiles.forEachIndexed { index, tile ->
                    val isSideways =
                        (index == 0 && instance.position.next(playerSettings) == call.target) ||
                                (index == 1 && instance.position.center(playerSettings) == call.target) ||
                                (index == 3 && instance.position.previous(playerSettings) == call.target)
                    if (isSideways) {
                        addHorizontal(tile)
                    } else {
                        addVertical(tile)
                    }
                }
            }
            Type.CONCEALED_KAN -> {
                call.tiles.forEachIndexed { index, tile ->
                    if (index in 1..2) {
                        addVertical(tile, pitch = 90f)
                    } else {
                        addVertical(tile)
                    }
                }
            }
            Type.LATE_KAN -> {
                // ポンの位置に追加するだけ
                val tile = call.tiles.last()

                val sidewaysIndex = when (call.target) {
                    instance.position.next(playerSettings) -> 0
                    instance.position.center(playerSettings) -> 1
                    instance.position.previous(playerSettings) -> 2
                    else -> -1
                }

                repeat(sidewaysIndex) {
                    currentPosition.x += width
                }

                addPosition(tile, height / 2, height - width, yawAdjustment = -90f)
            }
        }

        val rotation = instance.position.getRadians(dealerPosition)

        return positions.map { (tile, location) ->
            tile to center.clone().add(location.toVector().rotateAroundY(rotation))
                .setPitchL(location.pitch).setYawL(location.yaw)
        }
    }

    fun getOnTimeChange(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("onTimeChange")
        } ?: emptyList()
    }

    fun getWinningTileLocation(center: Location, index: Int, position: Position, dealerPosition: Position, lastTile: Boolean): Location {
        val base = getPlacementLocation("winning.tile", center, index, position, dealerPosition)

        val lastTileOffset = if (lastTile) {
            getOrDefault { it.get3Point("winning.tile.offset.last") } ?: Vector(0, 0, 0)
        } else Vector(0, 0, 0)

        val rotation = position.getRadians(dealerPosition)
        return base.add(lastTileOffset.rotateAroundY(rotation))
    }

    fun getWinningTileTranslation(
        index: Int,
        position: Position,
        dealerPosition: Position,
        lastTile: Boolean
    ): Vector {
        val base = getTranslation("winning.tile", index, position, dealerPosition)

        val lastTileOffset = if (lastTile) {
            getOrDefault { it.get3Point("winning.tile.translation.last") } ?: Vector(0, 0, 0)
        } else Vector(0, 0, 0)

        val rotation = position.getRadians(dealerPosition)
        return base.add(lastTileOffset.rotateAroundY(rotation))
    }

    fun getOnWinning(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("winning.onWinning")
        } ?: emptyList()
    }

    fun getOnScoreChange(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("winning.onScoreChange")
        } ?: emptyList()
    }

    fun getRyukyokuDisplay(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("ryukyoku.actions")
        } ?: emptyList()
    }

    fun getRyukyokuTenpaiTileFixedLocation(center: Location, position: Position, dealerPosition: Position): Location {
        val fixedOffset = getOrDefault {
            it.get3Point("ryukyoku.tenpai.fixedOffset")
        } ?: Vector(0.0, 0.0, 0.0)

        val rotation = position.getRadians(dealerPosition)

        return center.clone().add(fixedOffset.rotateAroundY(rotation)).setYawL(position.getMinecraftYaw(dealerPosition))
    }

    fun getRyukyokuTenpaiTileLocations(
        center: Location,
        size: Int,
        position: Position,
        dealerPosition: Position
    ): List<Location> {
        val tileWidth = getTileWidth()
        val centerLocation = getRyukyokuTenpaiTileFixedLocation(center, position, dealerPosition)
        val distance = getOrDefault {
            it.get3Point("ryukyoku.tenpai.distance")
        } ?: Vector(0.0, 0.0, 0.0)

        val rotation = position.getRadians(dealerPosition)

        val locations = mutableListOf<Location>()
        repeat(size) { index ->
            val actualDistance = distance.clone().multiply((index - (size - 1) / 2.0)).rotateAroundY(rotation)
            val location = centerLocation.clone().add(actualDistance)
                .add(Vector(-tileWidth / 2.0, 0.0, 0.0).rotateAroundY(rotation))
                .setYawL(position.getMinecraftYaw(dealerPosition))
            locations.add(location)
        }

        return locations
    }

    fun getRyukyokuTenpaiTileFrameDisplay(): List<IAdvancedConfigurationSection> {
        return getOrDefault {
            it.getNullableActionList("ryukyoku.tenpai.frame")
        } ?: emptyList()
    }

    companion object: CostumeFactory<MahjongTableCostume> {
        override var default = MahjongMc.displayUtils.loadAdvancedConfiguration(
            File(SJavaPlugin.plugin.dataFolder, "costume/table/default.yml")
        )

        override fun create(): MahjongTableCostume {
            return MahjongTableCostume()
        }
    }
}