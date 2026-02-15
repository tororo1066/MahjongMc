package tororo1066.mahjongmc.game.ui

import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bukkit.Location
import tororo1066.displaymonitorapi.actions.IActionContext
import tororo1066.displaymonitorapi.configuration.IAdvancedConfigurationSection
import tororo1066.mahjongmc.Call
import tororo1066.mahjongmc.MahjongMc
import tororo1066.mahjongmc.costume.EffectCostume
import tororo1066.mahjongmc.costume.MahjongTableCostume
import tororo1066.mahjongmc.costume.TileCostume
import tororo1066.mahjongmc.costume.TileStateRenderType
import tororo1066.mahjongmc.enums.Position
import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.game.ui.MahjongDisplays.RICHI_CANCEL_DISPLAY
import tororo1066.mahjongmc.game.ui.MahjongDisplays.CHOICE_CANCEL_DISPLAY
import tororo1066.mahjongmc.game.ui.MahjongDisplays.CHOICE_DISPLAY
import tororo1066.mahjongmc.game.ui.MahjongDisplays.POSITION_DISPLAY
import tororo1066.mahjongmc.game.ui.MahjongDisplays.RYUKYOKU_DISPLAY
import tororo1066.mahjongmc.game.ui.MahjongDisplays.SCORE_CHANGE_DISPLAY
import tororo1066.mahjongmc.game.ui.MahjongDisplays.STATE_DISPLAY
import tororo1066.mahjongmc.game.ui.MahjongDisplays.TABLE_DISPLAY
import tororo1066.mahjongmc.game.ui.MahjongDisplays.TILE_DISPLAY
import tororo1066.mahjongmc.game.ui.MahjongDisplays.TIME_DISPLAY
import tororo1066.mahjongmc.game.ui.MahjongDisplays.WINNING_DISPLAY
import tororo1066.mahjongmc.game.ui.MahjongDisplays.appendSuffix
import tororo1066.mahjongmc.mahjong.AbstractWinning
import tororo1066.mahjongmc.mahjong.WinningStructure
import tororo1066.mahjongmc.tile.Tile
import tororo1066.mahjongmc.tile.TileSpawnType
import tororo1066.tororopluginapi.SJavaPlugin
import tororo1066.tororopluginapi.utils.setPitchL
import tororo1066.tororopluginapi.utils.setYawL
import kotlin.collections.set
import kotlin.random.Random

class MahjongRenderer(
    val instance: MahjongInstance
) {

    val centerLocation
        get() = instance.centerLocation.clone()

    private fun Position.getMinecraftYaw(): Float {
        return this.getMinecraftYaw(instance.dealerPosition, instance.settings.playerSettings)
    }

    private fun Position.getCenterFacingLocation(): Location {
        return centerLocation.setYawL(this.getMinecraftYaw())
    }

    suspend fun spawnTable() {
        val tasks = mutableListOf<Job>()
        instance.players.forEach { player ->
            val costume = player.getCostume<MahjongTableCostume>()
            tasks.add(
                MahjongMc.runActions(
                    costume.getOnSpawn(),
                    player.createContext(instance).apply {
                        location = player.position.getCenterFacingLocation()
                        prepareParameters["display"] = TABLE_DISPLAY
                    }
                )
            )
        }

        tasks.joinAll()
    }

    fun spawnTile(
        player: PlayerInstance,
        tile: Tile,
        index: Int,
        lastTile: Boolean = false,
        onInteract: (context: IActionContext) -> Unit
    ): Job {

        val tasks = mutableListOf<Job>()

        instance.players.forEach { receiver ->

            val samePlayer = receiver.uuid == player.uuid

            val tile = if (samePlayer) tile else Tile()

            if (!samePlayer) {
                receiver.otherPlayerDisplayTiles.getOrPut(player.uuid) { mutableListOf() }.add(tile)
            }

            val display = TILE_DISPLAY.appendSuffix(tile.uuid.toString())
            val costume = receiver.getCostume<MahjongTableCostume>()
            val tileCostume = receiver.getCostume<TileCostume>()

            val task = MahjongMc.runActions(
                tileCostume.getOnSpawn(),
                receiver.createContext(instance).apply {
                    this.location = costume.getTileLocation(
                        center = instance.centerLocation,
                        tileIndex = index,
                        position = player.position,
                        dealerPosition = instance.dealerPosition,
                        lastTile = lastTile
                    )
                    this.prepareParameters.let {
                        it["tile"] = tile.parameters()
                        it["display"] = display
                        it["index"] = index
                        it["hidden"] = !samePlayer
                        it["type"] = TileSpawnType.HAND.lowercase()
                    }
                }
            )
            task.invokeOnCompletion { throwable ->
                if (throwable == null) {
                    if (samePlayer) {
                        MahjongMc.injectOnInteract(
                            receiver.publicActionContext,
                            display,
                            onInteract
                        )
                    }
                }
            }
            tasks.add(task)
        }

        return instance.scope.launch {
            tasks.joinAll()
        }
    }

    suspend fun spawnPrepareTiles(range: IntRange, onInteract: (player: PlayerInstance, tile: Tile) -> Unit) {
        val tasks = mutableListOf<Job>()

        instance.players.forEach { player ->
            range.forEach { index ->
                val tile = player.tiles[index]
                val spawnTileTask = spawnTile(
                    tile = tile,
                    index = index,
                    player = player,
                    lastTile = false
                ) { context ->
                    if (context.target?.uniqueId != player.uuid) return@spawnTile
                    onInteract(player, tile)
                }

                tasks.add(spawnTileTask)
            }
        }

        tasks.joinAll()
    }

    suspend fun spawnDeadWallTiles() {
        val tasks = mutableListOf<Job>()

        instance.players.forEach { receiver ->
            val costume = receiver.getCostume<MahjongTableCostume>()
            val tileCostume = receiver.getCostume<TileCostume>()
            instance.deadWall.displayTiles.forEachIndexed { index, tile ->
                val opened = instance.deadWall.openedBonusTiles.contains(tile)
                val tileLocation = costume.getDeadWallTileLocation(
                    center = centerLocation,
                    tileIndex = index,
                    position = receiver.position,
                    dealerPosition = instance.dealerPosition,
                    opened = opened
                )

                val task = MahjongMc.runActions(
                    tileCostume.getOnSpawn(),
                    receiver.createContext(instance).apply {
                        location = tileLocation
                        prepareParameters.let {
                            it["tile"] = tile.parameters()
                            it["display"] = TILE_DISPLAY.appendSuffix(tile.uuid.toString())
                            it["hidden"] = !opened
                            it["type"] = TileSpawnType.DEAD_WALL.lowercase()
                        }
                    }
                )
                tasks.add(task)
            }
        }

        tasks.joinAll()
    }

    fun asyncSpawnActionDisplay(
        player: PlayerInstance,
        name: String,
        index: Int,
        actions: List<IAdvancedConfigurationSection>,
        onInteract: (context: IActionContext) -> Unit
    ): Job {

        val costume = player.getCostume<MahjongTableCostume>()

        val context = player.createContext(instance).apply {
            location = costume.getActionDisplayPosition(
                center = centerLocation,
                index = index,
                position = player.position,
                dealerPosition = instance.dealerPosition
            )
            prepareParameters["display"] = name
        }

        val job = MahjongMc.runActions(
            actions,
            context
        )

        job.invokeOnCompletion { throwable ->
            if (throwable == null) {
                MahjongMc.injectOnInteract(
                    publicActionContext = player.publicActionContext,
                    name = name,
                    onInteract = onInteract
                )
            }
        }

        return job
    }

    suspend fun removeDisplays(player: PlayerInstance, prefix: String) {
        val context = player.publicActionContext
        withContext(SJavaPlugin.plugin.minecraftDispatcher) {
            context.elements.entries.removeIf { (key, element) ->
                if (key.startsWith(prefix)) {
                    element.remove()
                    true
                } else {
                    false
                }
            }
        }
    }

    fun asyncRemoveDisplays(player: PlayerInstance, prefix: String): Job {
        return instance.minecraftScope.launch {
            removeDisplays(player, prefix)
        }
    }

    suspend fun renderTimeDisplay(
        player: PlayerInstance,
        currentTime: Int,
        holdTime: Int,
        backupTime: Int
    ) {
        val costume = player.getCostume<MahjongTableCostume>()
        val context = player.createContext(instance).apply {
            location = player.position.getCenterFacingLocation()
            prepareParameters["display"] = TIME_DISPLAY
            prepareParameters["time"] = currentTime
            prepareParameters["holdTime"] = holdTime
            prepareParameters["backupTime"] = backupTime
        }

        MahjongMc.runActions(
            costume.getOnTimeChange(),
            context
        ).join()
    }

    suspend fun renderDiscardTile(
        player: PlayerInstance,
        tile: Tile,
        isLastTile: Boolean,
        richiTile: Int,
        beforeTiles: List<Tile>
    ) {
        val tasks = mutableListOf<Job>()

        instance.players.forEach { receiver ->
            val samePlayer = receiver == player
            val costume = receiver.getCostume<MahjongTableCostume>()
            val tileCostume = receiver.getCostume<TileCostume>()

            val targetLocation = costume.getDiscardLocation(
                center = centerLocation,
                discardIndex = player.discard.size - 1,
                position = player.position,
                dealerPosition = instance.dealerPosition
            )

            if (tile.isRichiTile) {
                var previous = 0
                for (element in costume.getDiscardSplit()) {
                    val range = previous..element
                    if (richiTile in range && player.discard.size - 1 in range) {
                        targetLocation.add(costume.getDiscardRichiOffset())
                        break
                    }
                    previous = element
                }
            }

            val otherPlayerDisplayTiles = receiver.otherPlayerDisplayTiles.getOrPut(player.uuid) { mutableListOf() }
            val beforeOtherPlayerDisplayTiles = otherPlayerDisplayTiles.toList()
            val discardTile = if (samePlayer) {
                tile
            } else {
                val discardTile = if (isLastTile) {
                    otherPlayerDisplayTiles.removeLast()
                } else {
                    otherPlayerDisplayTiles.removeAt(Random.nextInt(otherPlayerDisplayTiles.size - 1))
                }

                receiver.otherPlayerDisplayDiscard.getOrPut(player.uuid) { mutableListOf() }.add(discardTile)
                discardTile.copyTileInfo(tile)
            }

            val tileDisplay = TILE_DISPLAY.appendSuffix(discardTile.uuid.toString())

            if (!samePlayer) {
                val renderTask = MahjongMc.runActions(
                    tileCostume.getOnRender(),
                    receiver.createContext(instance).apply {
                        location = targetLocation
                        prepareParameters.let {
                            it["tile"] = discardTile.parameters()
                            it["display"] = tileDisplay
                        }
                    }
                )

                tasks.add(renderTask)
            }

            val task = MahjongMc.runActions(
                costume.getOnDiscard(),
                receiver.createContext(instance).apply {
                    location = targetLocation
                    prepareParameters.let {
                        it["tile"] = discardTile.parameters()
                        it["display"] = tileDisplay
                        it["isRichi"] = tile.isRichiTile
                        it["hidden"] = !samePlayer
                    }
                }
            )

            task.invokeOnCompletion { throwable ->
                if (throwable == null) {
                    sortTilesAnimation(player, receiver, if (samePlayer) beforeTiles else beforeOtherPlayerDisplayTiles)
                }
            }

            tasks.add(task)
        }

        tasks.joinAll()
    }

    fun sortTilesAnimation(player: PlayerInstance, receiver: PlayerInstance, beforeTiles: List<Tile>): Job {
        val currentTiles = if (player == receiver) {
            player.tiles
        } else {
            receiver.otherPlayerDisplayTiles[player.uuid] ?: mutableListOf()
        }
        val costume = receiver.getCostume<MahjongTableCostume>()
        val tileCostume = receiver.getCostume<TileCostume>()

        val tasks = mutableListOf<Job>()

        currentTiles.forEachIndexed { index, tile ->
            if (beforeTiles.indexOf(tile) == index) return@forEachIndexed
            val task = MahjongMc.runActions(
                tileCostume.getOnMove(),
                receiver.createContext(instance).apply {
                    location = costume.getTileLocation(
                        center = centerLocation,
                        tileIndex = index,
                        position = player.position,
                        dealerPosition = instance.dealerPosition
                    )

                    prepareParameters.let {
                        it["tile"] = tile.parameters()
                        it["display"] = TILE_DISPLAY.appendSuffix(tile.uuid.toString())
                        it["hidden"] = receiver != player
                    }
                }
            )

            tasks.add(task)
        }

        return instance.scope.launch {
            tasks.joinAll()
        }
    }

    fun renderRevealBonusTile(
        tile: Tile
    ): Job {
        val tasks = mutableListOf<Job>()

        instance.players.forEach { receiver ->
            val costume = receiver.getCostume<MahjongTableCostume>()
            val tileCostume = receiver.getCostume<TileCostume>()
            val tileIndex = instance.deadWall.displayTiles.indexOf(tile)
            val tileLocation = costume.getDeadWallTileLocation(
                center = centerLocation,
                tileIndex = tileIndex,
                position = receiver.position,
                dealerPosition = instance.dealerPosition,
                opened = true
            )

            val parameters = mapOf(
                "tile" to tile.parameters(),
                "display" to TILE_DISPLAY.appendSuffix(tile.uuid.toString())
            )

            val renderTask = MahjongMc.runActions(
                tileCostume.getOnRender(),
                receiver.createContext(instance).apply {
                    location = tileLocation
                    prepareParameters.putAll(parameters)
                }
            )

            val moveTask = MahjongMc.runActions(
                tileCostume.getOnMove(),
                receiver.createContext(instance).apply {
                    location = tileLocation
                    prepareParameters.putAll(parameters)
                }
            )

            tasks.addAll(listOf(renderTask, moveTask))
        }

        return instance.scope.launch {
            tasks.joinAll()
        }
    }

    suspend fun callTiles(
        caller: PlayerInstance,
        discardPlayer: PlayerInstance,
        call: Call,
        step: Int,
        beforeTiles: List<Tile>,
        discardTile: Tile?
    ) {
        val tasks = mutableListOf<Job>()

        instance.players.forEach { receiver ->
            val costume = receiver.getCostume<MahjongTableCostume>()
            val tileCostume = receiver.getCostume<TileCostume>()
            val isCaller = receiver == caller
            val isDiscardedPlayer = receiver == discardPlayer

            val receiverTasks = mutableListOf<Job>()

            val beforeTiles = if (isCaller) {
                beforeTiles
            } else {
                receiver.otherPlayerDisplayTiles[caller.uuid]?.toList() ?: listOf()
            }
            val locations = costume.getCallTileLocations(
                center = centerLocation,
                call = call,
                instance = caller,
                position = step,
                dealerPosition = instance.dealerPosition
            )
            locations.forEach { (tile, tileLocation) ->

                val displayTile = if (tile == discardTile) {
                    if (isDiscardedPlayer) {
                        tile
                    } else {
                        receiver.otherPlayerDisplayDiscard.getOrPut(discardPlayer.uuid) { mutableListOf() }
                            .removeLast()
                            .copyTileInfo(tile)
                    }
                } else {
                    if (isCaller) {
                        tile
                    } else {
                        val tiles = receiver.otherPlayerDisplayTiles.getOrPut(caller.uuid) { mutableListOf() }
                        tiles.removeAt(Random.nextInt(tiles.size)).copyTileInfo(tile)
                    }
                }

                val tileDisplay = TILE_DISPLAY.appendSuffix(displayTile.uuid.toString())

                if ((!isDiscardedPlayer && tile == discardTile) || (!isCaller && tile != discardTile)) {
                    val renderTask = MahjongMc.runActions(
                        tileCostume.getOnRender(),
                        receiver.createContext(instance).apply {
                            location = tileLocation
                            prepareParameters.let {
                                it["tile"] = displayTile.parameters()
                                it["display"] = tileDisplay
                            }
                        }
                    )
                    receiverTasks.add(renderTask)
                }

                val task = MahjongMc.runActions(
                    tileCostume.getOnMove(),
                    receiver.createContext(instance).apply {
                        location = tileLocation
                        prepareParameters.let {
                            it["tile"] = displayTile.parameters()
                            it["display"] = tileDisplay
                        }
                    }
                )
                receiverTasks.add(task)
            }

            val combinedTask = instance.scope.launch {
                receiverTasks.joinAll()
                sortTilesAnimation(caller, receiver, beforeTiles).join()
            }
            tasks.add(combinedTask)
        }

        tasks.joinAll()
    }

    fun asyncConcealDisplays(player: PlayerInstance, displays: List<String>): Job {
        val costume = player.getCostume<MahjongTableCostume>()

        return MahjongMc.runActions(
            costume.getConcealDisplay(),
            player.createContext(instance).apply {
                location = player.position.getCenterFacingLocation()
                prepareParameters["displays"] = displays
            }
        )
    }

    fun asyncRevealDisplays(player: PlayerInstance, displays: List<String>): Job {
        val costume = player.getCostume<MahjongTableCostume>()

        return MahjongMc.runActions(
            costume.getRevealDisplay(),
            player.createContext(instance).apply {
                location = player.position.getCenterFacingLocation()
                prepareParameters["displays"] = displays
            }
        )
    }

    fun renderChoiceCallDisplays(
        player: PlayerInstance,
        possibleCalls: List<Call>,
        onChosen: (call: Call) -> Unit,
        onCanceled: () -> Unit
    ) {
        val mahjongTableCostume = player.getCostume<MahjongTableCostume>()
        val tileCostume = player.getCostume<TileCostume>()

        MahjongMc.runActions(
            mahjongTableCostume.getChoicesDisplay(),
            player.createContext(instance).apply {
                location = mahjongTableCostume.getChoicesCenterLocation(
                    center = centerLocation,
                    position = player.position,
                    dealerPosition = instance.dealerPosition
                )
                prepareParameters["display"] = CHOICE_DISPLAY
                prepareParameters["cancelDisplay"] = CHOICE_CANCEL_DISPLAY
                prepareParameters["choices"] = possibleCalls.size
            }
        ).also { job ->
            job.invokeOnCompletion { throwable ->
                if (throwable == null) {
                    MahjongMc.injectOnInteract(
                        publicActionContext = player.publicActionContext,
                        name = CHOICE_CANCEL_DISPLAY
                    ) { _ ->
                        onCanceled()
                    }
                }
            }
        }

        possibleCalls.forEachIndexed { index, call ->
            val tiles = call.tiles.filter { it != call.calledTile }
            val locations = mahjongTableCostume.getChoicesTileLocations(
                center = centerLocation,
                index = index,
                size = possibleCalls.size,
                callType = call.type,
                position = player.position,
                dealerPosition = instance.dealerPosition
            )

            fun spawnTileAtLocation(tile: Tile, location: Location, displaySuffix: String): Job {
                val display = CHOICE_DISPLAY.appendSuffix("${index}_$displaySuffix")
                return MahjongMc.runActions(
                    tileCostume.getOnSpawn(),
                    player.createContext(instance).apply {
                        this.location = location
                        prepareParameters.let {
                            it["tile"] = tile.parameters()
                            it["display"] = display
                            it["type"] = TileSpawnType.CHOICE.lowercase()
                            it["hidden"] = false
                        }
                    }
                ).also { job ->
                    job.invokeOnCompletion { throwable ->
                        if (throwable == null) {
                            MahjongMc.injectOnInteract(
                                publicActionContext = player.publicActionContext,
                                name = display
                            ) { _ ->
                                onChosen(call)
                            }
                        }
                    }
                }
            }

            locations.forEachIndexed { tileIndex, location ->
                spawnTileAtLocation(tiles[tileIndex], location, "$tileIndex")
            }
        }
    }

    enum class WinningType {
        TSUMO,
        RON,
        NAGASHI_MANGAN
    }

    suspend fun renderWinning(
        player: PlayerInstance,
        winnings: List<AbstractWinning>,
        winningTile: Tile?,
        score: Int,
        type: WinningType,
        getHan: AbstractWinning.() -> Int?
    ) {
        val tiles = (player.tiles + player.calls.flatMap { it.tiles }).toMutableList()
            .sortedWith(compareBy({ it.type.ordinal }, { it.number }, { it.honor.ordinal }))
            .map { Tile().copyTileInfo(it) }

        val winningsParameters = winnings.map { winning ->
            mapOf(
                "name" to winning.name,
                "han" to winning.getHan()
            )
        }

        val tasks = mutableListOf<Job>()

        instance.players.forEach { receiver ->
            val costume = receiver.getCostume<MahjongTableCostume>()
            val tileCostume = receiver.getCostume<TileCostume>()

            val task = MahjongMc.runActions(
                costume.getOnWinning(),
                receiver.createContext(instance).apply {
                    location = player.position.getCenterFacingLocation()
                    prepareParameters.let {
                        it["display"] = WINNING_DISPLAY
                        it["tiles"] = tiles.map { tile -> tile.parameters() }
                        it["winningTile"] = winningTile?.parameters()
                        it["winnings"] = winningsParameters
                        it["score"] = score
                        it["type"] = type.name.lowercase()
                        it["player"] = player.uuid.toString()
                    }
                }
            )

            tasks.add(task)

            tiles.forEachIndexed { index, tile ->
                val isLastTile = type != WinningType.NAGASHI_MANGAN && index == tiles.size - 1
                val display = WINNING_DISPLAY.appendSuffix(tile.uuid.toString())
                val translation = costume.getWinningTileTranslation(
                    index = index,
                    position = player.position,
                    dealerPosition = instance.dealerPosition,
                    lastTile = isLastTile
                )
                val task = MahjongMc.runActions(
                    tileCostume.getOnSpawn(),
                    receiver.createContext(instance).apply {
                        location = costume.getWinningTileLocation(
                            center = centerLocation,
                            index = index,
                            position = player.position,
                            dealerPosition = instance.dealerPosition,
                            lastTile = isLastTile
                        )
                        prepareParameters.let {
                            it["type"] = TileSpawnType.WINNING.lowercase()
                            it["tile"] = tile.parameters()
                            it["display"] = display
                            it["translation.x"] = translation.x
                            it["translation.y"] = translation.y
                            it["translation.z"] = translation.z
                        }
                    }
                )

                tasks.add(task)
            }
        }

        tasks.joinAll()
        delay(60.ticks)

        instance.players.forEach { receiver ->
            removeDisplays(receiver, WINNING_DISPLAY)
        }
    }

    suspend fun renderScoreChange(
        beforeScores: Map<PlayerInstance, Int>
    ) {
        val tasks = mutableListOf<Job>()
        instance.players.forEach { receiver ->
            val costume = receiver.getCostume<MahjongTableCostume>()
            val task = MahjongMc.runActions(
                costume.getOnScoreChange(),
                receiver.createContext(instance).apply {
                    location = centerLocation.setYawL(receiver.position.getMinecraftYaw())
                    prepareParameters.let {
                        it["display"] = SCORE_CHANGE_DISPLAY
                        it["beforeScores"] = beforeScores.entries.associate { (player, score) ->
                            player.uuid.toString() to score
                        }
                        it["afterScores"] = instance.players.associate { player ->
                            player.uuid.toString() to player.score
                        }
                    }
                }
            )

            tasks.add(task)
        }

        tasks.joinAll()
        delay(60.ticks)

        instance.players.forEach { receiver ->
            removeDisplays(receiver, SCORE_CHANGE_DISPLAY)
        }
    }

    private fun asyncRenderTileState(
        player: PlayerInstance,
        tiles: List<Tile>,
        renderType: TileStateRenderType
    ): Job {
        val tileCostume = player.getCostume<TileCostume>()

        val tasks = tiles.map { tile ->
            MahjongMc.runActions(
                tileCostume.renderTileState(),
                player.createContext(instance).apply {
                    location = player.position.getCenterFacingLocation()
                    prepareParameters["display"] = TILE_DISPLAY.appendSuffix(tile.uuid.toString())
                    prepareParameters["tile"] = tile.parameters()
                    prepareParameters["type"] = renderType.name.lowercase()
                }
            )
        }

        return instance.scope.launch {
            tasks.joinAll()
        }
    }

    fun asyncRenderRemoveStateTiles(
        player: PlayerInstance,
        tiles: List<Tile>
    ): Job {
        return asyncRenderTileState(
            player = player,
            tiles = tiles,
            renderType = TileStateRenderType.REMOVE
        )
    }

    fun asyncRenderKuigaeTiles(
        player: PlayerInstance,
        tiles: List<Tile>
    ): Job {
        return asyncRenderTileState(
            player = player,
            tiles = tiles,
            renderType = TileStateRenderType.KUIGAE
        )
    }

    fun asyncRenderRichiableTiles(
        player: PlayerInstance,
        richiableTiles: List<Tile>,
        notRichiableTiles: List<Tile>,
        onCancel: () -> Unit
    ): Job {
        val costume = player.getCostume<MahjongTableCostume>()

        val tasks = mutableListOf<Job>()

        val cancelTask = MahjongMc.runActions(
            costume.getCancelDisplay(),
            player.createContext(instance).apply {
                location = player.position.getCenterFacingLocation()
                prepareParameters["display"] = RICHI_CANCEL_DISPLAY
            }
        )

        cancelTask.invokeOnCompletion { throwable ->
            if (throwable == null) {
                MahjongMc.injectOnInteract(
                    publicActionContext = player.publicActionContext,
                    name = RICHI_CANCEL_DISPLAY
                ) { _ ->
                    onCancel()
                    asyncRemoveDisplays(player, RICHI_CANCEL_DISPLAY)
                }
            }
        }

        tasks.add(cancelTask)

        tasks.add(
            asyncRenderTileState(
                player = player,
                tiles = richiableTiles,
                renderType = TileStateRenderType.RICHI
            )
        )

        tasks.add(
            asyncRenderTileState(
                player = player,
                tiles = notRichiableTiles,
                renderType = TileStateRenderType.RICHI_DENY
            )
        )

        return instance.scope.launch {
            tasks.joinAll()
        }
    }

    fun asyncLayTilesDown(
        player: PlayerInstance,
        receivers: List<PlayerInstance> = instance.players,
        tiles: List<Tile> = player.tiles,
        isTsumo: Boolean = false,
        hidden: Boolean = false
    ): Job {
        val tasks = mutableListOf<Job>()

        receivers.forEach { receiver ->
            val samePlayer = receiver == player
            val costume = receiver.getCostume<MahjongTableCostume>()
            val tileCostume = receiver.getCostume<TileCostume>()

            tiles.forEach { tile ->
                val index = player.tiles.indexOf(tile)
                if (index == -1) return@forEach
                val renderTile = if (samePlayer) {
                    tile
                } else {
                    receiver.otherPlayerDisplayTiles.getOrPut(player.uuid) { mutableListOf() }[index]
                        .copyTileInfo(tile)
                }

                val display = TILE_DISPLAY.appendSuffix(renderTile.uuid.toString())

                val location = costume.getTileLocation(
                    center = centerLocation,
                    tileIndex = index,
                    position = player.position,
                    dealerPosition = instance.dealerPosition,
                    lastTile = isTsumo && index == tiles.size - 1
                ).setPitchL(if (hidden) 90f else -90f)

                if (!samePlayer) {
                    val renderTask = MahjongMc.runActions(
                        tileCostume.getOnRender(),
                        receiver.createContext(instance).apply {
                            this.location = location
                            prepareParameters.let {
                                it["tile"] = renderTile.parameters()
                                it["display"] = display
                            }
                        }
                    )

                    tasks.add(renderTask)
                }

                val moveTask = MahjongMc.runActions(
                    tileCostume.getOnMove(),
                    receiver.createContext(instance).apply {
                        this.location = location
                        prepareParameters.let {
                            it["tile"] = renderTile.parameters()
                            it["display"] = display
                        }
                    }
                )

                tasks.add(moveTask)
            }
        }

        return instance.scope.launch {
            tasks.joinAll()
        }
    }

    suspend fun renderRyukyoku(tenpaiTiles: Map<PlayerInstance, List<Tile>>) {
        // 手牌を倒す
        instance.players.map { player ->
            asyncLayTilesDown(
                player = player,
                hidden = !tenpaiTiles.containsKey(player)
            )
        }.joinAll()

        instance.players.map { player ->
            val costume = player.getCostume<MahjongTableCostume>()
            MahjongMc.runActions(
                costume.getRyukyokuDisplay(),
                player.createContext(instance).apply {
                    location = player.position.getCenterFacingLocation()
                    prepareParameters.let {
                        it["display"] = RYUKYOKU_DISPLAY
                        it["tenpaiPlayers"] = tenpaiTiles.entries.associate { (player, tiles) ->
                            player.uuid.toString() to tiles.map { tile -> tile.parameters() }
                        }
                    }
                }
            )
        }.joinAll()

        tenpaiTiles.flatMap { (player, tiles) ->
            instance.players.flatMap { receiver ->
                val costume = receiver.getCostume<MahjongTableCostume>()
                val tileCostume = receiver.getCostume<TileCostume>()
                val tenpaiTileLocations = costume.getRyukyokuTenpaiTileLocations(
                    center = centerLocation,
                    size = tiles.size,
                    position = player.position,
                    dealerPosition = instance.dealerPosition
                )

                val tasks = mutableListOf<Job>()

                val frameTask = MahjongMc.runActions(
                    costume.getRyukyokuTenpaiTileFrameDisplay(),
                    receiver.createContext(instance).apply {
                        location = costume.getRyukyokuTenpaiTileFixedLocation(
                            center = centerLocation,
                            position = player.position,
                            dealerPosition = instance.dealerPosition
                        )
                        prepareParameters.let {
                            it["display"] = RYUKYOKU_DISPLAY.appendSuffix("${player.uuid}_frame")
                            it["size"] = tiles.size
                        }
                    }
                )

                tasks.add(frameTask)

                tiles.forEachIndexed { index, tile ->
                    val location = tenpaiTileLocations[index]
                    val display = RYUKYOKU_DISPLAY.appendSuffix(tile.uuid.toString())
                    val spawnTask = MahjongMc.runActions(
                        tileCostume.getOnSpawn(),
                        receiver.createContext(instance).apply {
                            this.location = location
                            prepareParameters.let {
                                it["tile"] = tile.parameters()
                                it["display"] = display
                                it["type"] = TileSpawnType.TENPAI.lowercase()
                                it["hidden"] = false
                            }
                        }
                    )

                    tasks.add(spawnTask)
                }

                tasks
            }
        }.joinAll()

        delay(60.ticks)

        instance.players.forEach { receiver ->
            removeDisplays(receiver, RYUKYOKU_DISPLAY)
        }
    }

    suspend fun renderTsumo(
        player: PlayerInstance,
        winningTile: Tile
    ) {
        val effectCostume = player.getCostume<EffectCostume>()
        val tasks = mutableListOf<Job>()
        instance.players.forEach { receiver ->
            val samePlayer = receiver == player
            val costume = receiver.getCostume<MahjongTableCostume>()

            val targetTile = if (samePlayer) {
                winningTile
            } else {
                receiver.otherPlayerDisplayTiles.getOrPut(player.uuid) { mutableListOf() }
                    .last()
                    .copyTileInfo(winningTile)
            }
            val display = TILE_DISPLAY.appendSuffix(targetTile.uuid.toString())

            val effectTask = MahjongMc.runActions(
                effectCostume.getTsumoEffects(),
                receiver.createContext(instance).apply {
                    location = costume.getTileLocation(
                        center = centerLocation,
                        tileIndex = player.tiles.size - 1,
                        position = player.position,
                        dealerPosition = instance.dealerPosition,
                        lastTile = true
                    )
                    prepareParameters.let {
                        it["tile"] = winningTile.parameters()
                        it["display"] = display
                    }
                }
            )

            tasks.add(instance.scope.launch {
                effectTask.join()
                asyncLayTilesDown(
                    player = player,
                    receivers = listOf(receiver),
                    tiles = player.tiles,
                    isTsumo = true
                ).join()
            })
        }

        tasks.joinAll()
    }

    suspend fun renderRon(
        players: List<PlayerInstance>,
        targetPlayer: PlayerInstance,
        winningTile: Tile
    ) {
        val firstPlayer = players.first()
        val effectCostume = firstPlayer.getCostume<EffectCostume>()
        val tasks = mutableListOf<Job>()
        instance.players.forEach { receiver ->
            val isTargetPlayer = targetPlayer == receiver
            val costume = receiver.getCostume<MahjongTableCostume>()
            val targetTile = if (isTargetPlayer) {
                winningTile
            } else {
                receiver.otherPlayerDisplayDiscard.getOrPut(targetPlayer.uuid) { mutableListOf() }
                    .last()
                    .copyTileInfo(winningTile)
            }
            val display = TILE_DISPLAY.appendSuffix(targetTile.uuid.toString())

            val callEffectTasks = players.map { player ->
                val effectCostume = player.getCostume<EffectCostume>()
                MahjongMc.runActions(
                    effectCostume.getRonEffectsCall(),
                    receiver.createContext(instance).apply {
                        location = player.position.getCenterFacingLocation()
                        prepareParameters.let {
                            it["tile"] = winningTile.parameters()
                            it["display"] = display
                        }
                    }
                )
            }

            val effectTask = MahjongMc.runActions(
                effectCostume.getRonEffects(),
                receiver.createContext(instance).apply {
                    location = costume.getDiscardLocation(
                        center = centerLocation,
                        discardIndex = targetPlayer.discard.size - 1,
                        position = targetPlayer.position,
                        dealerPosition = instance.dealerPosition
                    )
                    prepareParameters.let {
                        it["tile"] = winningTile.parameters()
                        it["display"] = display
                    }
                }
            )

            tasks.add(instance.scope.launch {
                callEffectTasks.joinAll()
                effectTask.join()
                asyncLayTilesDown(
                    player = firstPlayer,
                    receivers = listOf(receiver),
                    tiles = firstPlayer.tiles,
                    isTsumo = false
                ).join()
            })
        }

        tasks.joinAll()
    }

    fun asyncRenderPosition(): Job {
        val tasks = mutableListOf<Job>()

        instance.players.forEach { receiver ->
            val costume = receiver.getCostume<MahjongTableCostume>()
            instance.players.forEach { player ->
                val task = MahjongMc.runActions(
                    costume.getOnRenderPosition(),
                    receiver.createContext(instance).apply {
                        location = player.position.getCenterFacingLocation()
                        prepareParameters.let {
                            it["display"] = POSITION_DISPLAY.appendSuffix(player.uuid.toString())
                            it["player"] = player.uuid.toString()
                            it["position"] = player.position.displayName
                        }
                    }
                )

                tasks.add(task)
            }
        }

        return instance.scope.launch {
            tasks.joinAll()
        }
    }

    fun asyncRenderState(): Job {
        val tasks = mutableListOf<Job>()

        val parameters = mapOf(
            "round" to (instance.round - 1) % 4 + 1,
            "roundWind" to instance.roundWind.name,
            "dealerPosition" to instance.dealerPosition.name,
            "honba" to instance.continueCount,
            "richiSticks" to instance.richiSticks,
            "remainingTiles" to instance.generalTiles.size
        )

        instance.players.forEach { player ->
            val costume = player.getCostume<MahjongTableCostume>()
            val task = MahjongMc.runActions(
                costume.getOnStateChange(),
                player.createContext(instance).apply {
                    location = player.position.getCenterFacingLocation()
                    prepareParameters.let {
                        it["display"] = STATE_DISPLAY
                        it.putAll(parameters)
                    }
                }
            )

            tasks.add(task)
        }

        return instance.scope.launch {
            tasks.joinAll()
        }
    }
}
