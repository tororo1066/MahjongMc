//package tororo1066.mahjongmc
//
//import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
//import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
//import com.github.shynixn.mccoroutine.bukkit.ticks
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.SupervisorJob
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import org.bukkit.Location
//import tororo1066.displaymonitor.elements.builtin.DisplayBaseElement
//import tororo1066.displaymonitor.storage.ActionStorage
//import tororo1066.displaymonitorapi.actions.IActionContext
//import tororo1066.displaymonitorapi.configuration.Execute
//import tororo1066.displaymonitorapi.configuration.IAdvancedConfigurationSection
//import tororo1066.mahjongmc.enums.Position
//import tororo1066.mahjongmc.mahjong.AbstractWinning
//import tororo1066.mahjongmc.mahjong.WinningManager
//import tororo1066.mahjongmc.mahjong.WinningStructure
//import tororo1066.mahjongmc.tile.Tile
//import tororo1066.tororopluginapi.SJavaPlugin
//import tororo1066.tororopluginapi.utils.setYawL
//import java.util.UUID
//import java.util.concurrent.CompletableFuture
//import java.util.concurrent.atomic.AtomicBoolean
//import kotlin.random.Random
//
//class MahjongInstance(
//    val location: Location,
//    val table: MahjongTableInstance
//) {
//
//    companion object {
//        const val TABLE_DISPLAY = "mahjong_table_display"
//        const val TILE_DISPLAY = "tile_display"
//        const val TIME_DISPLAY = "display_time"
//        const val WINNING_DISPLAY = "display_winning"
//        const val ACTION_DISPLAY = "display_action"
//        const val SKIP_DISPLAY = "display_action_skip"
//        const val PON_DISPLAY = "display_action_pon"
//        const val CHI_DISPLAY = "display_action_chi"
//        const val KAN_DISPLAY = "display_action_kan"
//        const val RON_DISPLAY = "display_action_ron"
//        const val REACH_DISPLAY = "display_action_reach"
//        const val TSUMO_DISPLAY = "display_action_tsumo"
//
//        fun String.appendSuffix(suffix: String): String {
//            return this + "_" + suffix
//        }
//    }
//
//    val scope = CoroutineScope(SupervisorJob() + SJavaPlugin.plugin.asyncDispatcher)
//
//    val players
//        get() = table.players
//    var turn: Position = Position.EAST
//    lateinit var turnPlayer: PlayerInstance
//
//    var roundWind: Position = Position.EAST //場風
//
//    var allowedInteract = AtomicBoolean(false)
//    var prepareActions = AtomicBoolean(false)
//    var resetRound = AtomicBoolean(false)
//
//    val displayUUIDCache = HashMap<UUID, ArrayList<UUID>>()
//
//    private fun setTurnPlayerInstance(player: PlayerInstance) {
//        turnPlayer = player
//        turn = player.position
//    }
//
//    private fun initRound() {
//        table.players.forEach { player ->
//            player.initRound()
//        }
//        table.setUpTiles()
//    }
//
//    fun start() {
//        scope.launch {
//            run()
//        }
//    }
//
//    suspend fun run() {
//
//        table.players.shuffle()
//        table.players.forEachIndexed { index, instance ->
//            instance.position = Position.entries[index]
//
//            if (instance.position == Position.EAST) {
//                turn = instance.position
//                turnPlayer = instance
//            }
//        }
//
//        table.spawnTable()
//
//        while (true) {
//            initRound()
//
//            withContext(SJavaPlugin.plugin.minecraftDispatcher) {
//                table.spawnPrepareTiles(0..3) { tile, context ->
//                    discardTile(tile, context)
//                }
//
//                delay(6.ticks)
//
//                table.spawnPrepareTiles(4..7) { tile, context ->
//                    discardTile(tile, context)
//                }
//
//                delay(6.ticks)
//
//                table.spawnPrepareTiles(8..11) { tile, context ->
//                    discardTile(tile, context)
//                }
//
//                delay(6.ticks)
//
//                table.spawnPrepareTiles(12..12) { tile, context ->
//                    discardTile(tile, context)
//                }
//            }
//
//            delay(12.ticks)
//
//            while (true) {
//                //牌の付与
//                val tile = table.generalTiles.removeFirst()
//                turnPlayer.tiles.add(tile)
//                tile.spawnEntity(
//                    table,
//                    turnPlayer.tiles.size - 1,
//                    turnPlayer,
//                    lastTile = true
//                ) { context ->
//                    removeDisplays(turnPlayer)
//                    discardTile(tile, context)
//                }
//
//                // TODO: ツモや立直、暗槓のアクション
//                var selfTileAction: SelfTileAction? = null
//                run {
//                    var index = 0
//                    val mahjongTableCostume = turnPlayer.mahjongTableCostume
//                    val canTsumo = turnPlayer.canTsumo()
//                    val canReach = false //TODO: 実装
//                    val canKan = false //TODO: 実装
//                    if (!canTsumo && !canReach && !canKan) return@run
//
//                    index += spawnDisplay(index, turnPlayer, SKIP_DISPLAY, mahjongTableCostume.getSkipDisplay()) { _ ->
//                        if (!allowedInteract.get()) return@spawnDisplay
//                        removeDisplays(turnPlayer)
//                    }
//
//                    if (canTsumo) {
//                        index += spawnDisplay(index, turnPlayer, TSUMO_DISPLAY, mahjongTableCostume.getTsumoDisplay()) { _ ->
//                            if (!allowedInteract.get()) return@spawnDisplay
//                            removeDisplays(turnPlayer)
//                            allowedInteract.set(false)
//                            selfTileAction = SelfTileAction.TSUMO
//                        }
//                    }
//                }
//                // TODO: End
//
//                allowedInteract.set(true)
//
//                val task = timeCountdown(turnPlayer, { !allowedInteract.get() }) {
//                    removeDisplays(turnPlayer)
//                    val lastTile = turnPlayer.tiles.last()
//                    discardTile(lastTile, turnPlayer)
//                }
//
//                task.join()
//
//                // TODO: 自己アクション処理
//                when (selfTileAction) {
//                    SelfTileAction.TSUMO -> {
//                        tsumo(turnPlayer)
//                        continue
//                    }
//
//                    else -> {}
//                }
//
//                if (resetRound.compareAndSet(true, false)) {
//                    break
//                }
//
//                if (prepareActions.compareAndSet(true, false)) {
//                    //ポン、チー、カンの処理
//                    val discardTile = turnPlayer.discard.last()
//
//                    val actablePlayers = mutableMapOf<PlayerInstance, ArrayList<ToTileAction>>()
//                    val actedPlayers = mutableMapOf<PlayerInstance, Pair<ToTileAction, Call?>>()
//                    var stop = true
//
//                    fun checkContinuable() {
//                        //ロンできるプレイやー全てが行動していたらtrue
//                        val ronPlayers = actablePlayers.filter { it.value.contains(ToTileAction.RON) }.keys
//                        if (ronPlayers.isNotEmpty()) {
//                            if (ronPlayers.all { actedPlayers.containsKey(it) }) {
//                                stop = true
//                            }
//                            return
//                        }
//
//                        //ロンできるプレイヤーがいない状況かつ、ポン・カンできるプレイヤーがしていたらtrue
//                        val ponKanPlayer = actablePlayers.entries.find { entry ->
//                            (entry.value.contains(ToTileAction.PON) || entry.value.contains(ToTileAction.KAN)) && actedPlayers.containsKey(entry.key)
//                        }
//                        if (ponKanPlayer != null) {
//                            stop = true
//                            return
//                        }
//
//                        //ロン、ポン・カンできるプレイヤーがいない状況かつ、チーできるプレイヤーがしていたらtrue
//                        val chiPlayer = actablePlayers.entries.find { entry ->
//                            entry.value.contains(ToTileAction.CHI) && actedPlayers.containsKey(entry.key)
//                        }
//                        if (chiPlayer != null) {
//                            stop = true
//                            return
//                        }
//
//                    }
//
//                    players.forEach { player ->
//                        if (player == turnPlayer) return@forEach
//                        var index = 0
//                        val mahjongTableCostume = player.mahjongTableCostume
//
//                        val canPon = player.canPon(discardTile)
//                        val canChi = false //TODO: 実装
//                        val canKan = false //TODO: 実装
//                        val canRon = false //TODO: 実装
//
//                        if (!canPon && !canChi && !canKan && !canRon) return@forEach
//
//                        stop = false
//
//                        timeCountdown(player, { actedPlayers.containsKey(player) }) {
//                            //タイムアウト時はスキップ
//                            if (actedPlayers.containsKey(player)) return@timeCountdown
//                            removeDisplays(player)
//                            actedPlayers[player] = ToTileAction.SKIP to null
//                            checkContinuable()
//                        }
//
//                        index += spawnDisplay(index, player, SKIP_DISPLAY, mahjongTableCostume.getSkipDisplay()) { _ ->
//                            if (actedPlayers.containsKey(player)) return@spawnDisplay
//                            removeDisplays(player)
//                            actedPlayers[player] = ToTileAction.SKIP to null
//                            checkContinuable()
//                        }
//
//                        if (canPon) {
//                            actablePlayers.getOrPut(player) { ArrayList() }.add(ToTileAction.PON)
//                            index += spawnDisplay(index, player, PON_DISPLAY, mahjongTableCostume.getPonDisplay()) { context ->
//                                if (actedPlayers.containsKey(player)) return@spawnDisplay
//                                removeDisplays(player)
//                                val choice = player.ponChoices(discardTile).first() //TODO: 選択肢対応
//                                actedPlayers[player] = ToTileAction.PON to Call(Call.Type.PON, choice, turnPlayer.position)
//                                checkContinuable()
//                            }
//                        }
//
//                        //TODO: チー、カン、ロンの表示と選択肢
//
//                    }
//
//                    //待機
//                    while (!stop) {
//                        delay(1.ticks)
//                    }
//
//                    //アクション処理
//                    //ロン > ポン・カン > チー
//                    val ronPlayers = actedPlayers.filter { it.value.first == ToTileAction.RON }
//                    if (ronPlayers.isNotEmpty()) {
//                        ronPlayers.forEach { (player, action) ->
//                            //TODO: ロンの処理
//                        }
//                    } else {
//                        //ポンとカンは1人しか起こせないので最初の人だけ処理
//                        val ponKanPlayer = actedPlayers.entries.find { it.value.first == ToTileAction.PON || it.value.first == ToTileAction.KAN }
//                        if (ponKanPlayer != null) {
//                            val (player, action) = ponKanPlayer
//                            val (actionType, call) = action.first to action.second!!
//                            when (actionType) {
//                                ToTileAction.PON -> {
//                                    callTiles(call, player, turnPlayer, discardTile)
//                                }
//
//                                ToTileAction.KAN -> {
//                                    //TODO: カンの処理
//                                }
//
//                                else -> {}
//                            }
//
//                            player.calls.add(call)
//                            setTurnPlayerInstance(player)
//                        } else {
////                            val chiPlayer = actedPlayers.entries.find { it.value == ToTileAction.CHI }
//                        }
//                    }
//                }
//
//                delay(20.ticks) //TODO: 仮
//
////                turn = Position.values()[(turn.ordinal + 1) % 4] //暫定
////                turnPlayer = players.first { it.position == turn }
//            }
//
//        }
//    }
//
//    private fun timeCountdown(player: PlayerInstance, cancelCondition: () -> Boolean, onTimeout: () -> Unit): Job {
//        var holdTime = player.holdTimes
//        var backupTime = player.backupTimes
//        var time = (holdTime + backupTime) * 20 //tick
//
//        fun removeDisplay() {
//            val context = table.publicActionContext
//            context.elements.entries.removeIf { entry ->
//                if (entry.key.startsWith(TIME_DISPLAY.appendSuffix(player.uuid.toString()))) {
//                    entry.value.remove()
//                    true
//                } else {
//                    false
//                }
//            }
//        }
//
//        val job = scope.launch {
//            var first = true
//            while (true) {
//                if (cancelCondition()) {
//                    removeDisplay()
//                    break
//                }
//                if (time <= 0) {
//                    removeDisplay()
//                    onTimeout()
//                    break
//                }
//
//                time--
//
//                if (time % 20 == 0) {
//                    if (backupTime <= 0) {
//                        holdTime--
//                    } else {
//                        backupTime--
//                    }
//                }
//
//                if (time % 20 == 0 || first) {
//                    first = false
//
//                    val costume = turnPlayer.mahjongTableCostume
//
//                    val isHoldTime = backupTime <= 0
//                    val timeSecond = time / 20
//
//                    MahjongMc.actionRunner.run(
//                        MahjongMc.emptyAdvancedConfiguration(),
//                        costume.getOnTimeChange(),
//                        MahjongMc.createActionContext(table.parameterizeContext()).apply {
//                            this.location = this@MahjongInstance.location
//                                .setYawL(-player.position.yaw)
//                            this.target = turnPlayer.getPlayer()
//                            prepareParameters.let {
//                                it["isHoldTime"] = isHoldTime.toString()
//                                it["remainingTime"] = timeSecond
//                                it["remainingHoldTime"] = holdTime
//                                it["remainingBackupTime"] = backupTime
//                                it["display"] = TIME_DISPLAY.appendSuffix(player.uuid.toString())
//                            }
//                        },
//                        null,
//                        true,
//                        false
//                    )
//                }
//                delay(1.ticks)
//            }
//        }
//
//        return job
//    }
//
//    suspend fun tsumo(player: PlayerInstance) {
//        val (winnings, winningStructure) = WinningManager.findBestWinningsSet(
//            instance = this,
//            player = player,
//            tiles = player.tiles,
//            winningTile = player.tiles.last(),
//            isTsumo = true
//        ) ?: return
//
//        //TODO: ツモ和了の演出
//
//        //TODO: ツモ和了の演出 END
//
//        val baseScore = WinningManager.getBaseScore(
//            instance = this,
//            player = player,
//            winnings = winnings,
//            winningStructure = winningStructure,
//            isTsumo = true
//        )
//
//        val (childScore, parentScore) = WinningManager.getScoreOnTsumo(
//            isChild = player.position != roundWind,
//            score = baseScore
//        )
//
//        var score = 0
//
//        players.forEach { p ->
//            if (p.uuid == player.uuid) return@forEach
//            val deducted = if (p.position == roundWind) parentScore else childScore
//            p.score -= deducted
//            score += deducted
//        }
//
//        player.score += score
//
//        agari(
//            player = player,
//            winnings = winnings,
//            winningStructure = winningStructure,
//            score = score,
//            isTsumo = true
//        )
//    }
//
//    suspend fun agari(
//        player: PlayerInstance,
//        winnings: List<AbstractWinning>,
//        winningStructure: WinningStructure,
//        score: Int,
//        isTsumo: Boolean
//    ) {
//        val tiles = (player.tiles.toMutableList() + player.calls.flatMap { it.tiles })
//            .sortedWith(compareBy({ it.type.ordinal }, { it.number }, { it.honor.ordinal }))
//            .map { Tile().copyTileInfo(it) }
//
//        val tasks = mutableListOf<CompletableFuture<Void>>()
//
//        players.forEach { p ->
//            val costume = p.mahjongTableCostume
//            val tileCostume = p.tileCostume
//
//            val task = MahjongMc.actionRunner.run(
//                MahjongMc.emptyAdvancedConfiguration(),
//                costume.getOnWinning(),
//                MahjongMc.createActionContext(table.parameterizeContext()).apply {
//                    location = this@MahjongInstance.location
//                    target = p.getPlayer()
//                    prepareParameters.let {
//                        it["display"] = WINNING_DISPLAY.appendSuffix(p.uuid.toString())
//                        it["tiles"] = tiles.map { tile -> tile.parameters() }
//                        it["winningTile"] = player.tiles.last().parameters()
//                        it["winnings"] = winnings.map { winning ->
//                            mapOf(
//                                "name" to winning.name,
//                                "han" to winning.getHan(
//                                    this@MahjongInstance,
//                                    player,
//                                    winningStructure,
//                                    isTsumo = isTsumo
//                                )
//                            )
//                        }
//                        it["score"] = score
//                        it["player"] = player.uuid.toString()
//                    }
//                    displayUUIDCache.getOrPut(p.uuid) { ArrayList() }.add(this.groupUUID)
//                },
//                null,
//                true,
//                false
//            )
//
//            tasks.add(task)
//
//            tiles.forEachIndexed { index, tile ->
//                MahjongMc.actionRunner.run(
//                    MahjongMc.emptyAdvancedConfiguration(),
//                    tileCostume.getOnSpawn(),
//                    MahjongMc.createActionContext(table.parameterizeContext()).apply {
//                        this.location = costume.getWinningTileLocation(
//                            this@MahjongInstance.location,
//                            index,
//                            player.position,
//                            lastTile = index == tiles.size - 1
//                        )
//                        this.target = p.getPlayer()
//                        prepareParameters.let {
//                            it["type"] = "result"
//                            it["tile"] = tile.parameters()
//                            it["display"] = TILE_DISPLAY.appendSuffix(tile.uuid.toString())
//                        }
//                        displayUUIDCache.getOrPut(p.uuid) { ArrayList() }.add(this.groupUUID)
//                    },
//                    null,
//                    true,
//                    false
//                )
//            }
//        }
//
//        CompletableFuture.allOf(*tasks.toTypedArray()).join()
//        delay(60.ticks)
//
//        table.publicActionContext.elements.entries.forEach { entry ->
//            if (entry.key.contains(WINNING_DISPLAY)) {
//                entry.value.remove()
//            }
//        }
//
//        resetRound.set(true)
//    }
//
//    fun callTiles(call: Call, caller: PlayerInstance, discardedPlayer: PlayerInstance, discardedTile: Tile) {
//        discardedPlayer.discard.remove(discardedTile)
//        val beforeTiles = caller.tiles.toList()
//        caller.tiles.removeAll(call.tiles.filter { it != discardedTile })
//
//        players.forEach { player ->
//            val costume = player.mahjongTableCostume
//            val tileCostume = player.tileCostume
//            val beforeTiles = if (player == caller) beforeTiles else player.otherPlayerDisplayTiles[caller.uuid]?.toList()
//            val locations = costume.getCallTileLocations(
//                this.location,
//                call,
//                caller
//            )
//            locations.forEach { (tile, location) ->
//                val tile = if (tile == discardedTile) {
//                    if (player.uuid == discardedPlayer.uuid) {
//                        tile
//                    } else {
//                        player.otherPlayerDisplayDiscard.getOrPut(discardedPlayer.uuid) { mutableListOf() }
//                            .removeLast().copyTileInfo(tile)
//                    }
//                } else {
//                    if (player.uuid == caller.uuid) {
//                        tile
//                    } else {
//                        val tiles = player.otherPlayerDisplayTiles.getOrPut(caller.uuid) { mutableListOf() }
//                        tiles.removeAt(Random.nextInt(tiles.size)).copyTileInfo(tile)
//                    }
//                }
//                MahjongMc.actionRunner.run(
//                    MahjongMc.emptyAdvancedConfiguration(),
//                    tileCostume.getOnRender(),
//                    MahjongMc.createActionContext(table.parameterizeContext()).apply {
//                        this.location = location
//                        this.target = player.getPlayer()
//                        prepareParameters.let {
//                            it["tile"] = tile.parameters()
//                            it["display"] = TILE_DISPLAY.appendSuffix(tile.uuid.toString())
//                        }
//                    },
//                    null,
//                    true,
//                    false
//                )
//                MahjongMc.actionRunner.run(
//                    MahjongMc.emptyAdvancedConfiguration(),
//                    tileCostume.getOnMove(),
//                    MahjongMc.createActionContext(table.parameterizeContext()).apply {
//                        this.location = location
//                        this.target = player.getPlayer()
//                        prepareParameters.let {
//                            it["tile"] = tile.parameters()
//                            it["display"] = TILE_DISPLAY.appendSuffix(tile.uuid.toString())
//                        }
//                    },
//                    null,
//                    true,
//                    false
//                )
//            }
//
//            table.sortTiles(
//                caller,
//                player,
//                beforeTiles
//            )
//        }
//    }
//
//    private fun spawnDisplay(index: Int, player: PlayerInstance, name: String, actions: List<IAdvancedConfigurationSection>, onInteract: (context: IActionContext) -> Unit): Int {
//        val displayName = name.appendSuffix(player.uuid.toString())
//
//        val context = MahjongMc.createActionContext(table.parameterizeContext()).apply {
//            this.location = player.mahjongTableCostume.getActionDisplayPosition(
//                this@MahjongInstance.location,
//                index,
//                player.position
//            )
//            this.target = player.getPlayer()
//            prepareParameters["display"] = displayName
//        }
//        MahjongMc.actionRunner.run(
//            MahjongMc.emptyAdvancedConfiguration(),
//            actions,
//            context,
//            null,
//            false,
//            false
//        )
//
//        //インタラクト設定
//        val elements = table.publicActionContext.allElements.filter { entry ->
//            entry.key.contains(displayName)
//        }
//        elements.forEach { entry ->
//            (entry.value as? DisplayBaseElement)?.onInteract = Execute { context ->
//                onInteract(context)
//            }
//        }
//
//        displayUUIDCache.getOrPut(player.uuid) { ArrayList() }.add(context.groupUUID)
//        return index + 1
//    }
//
//    private fun removeDisplays(player: PlayerInstance) {
//        val context = table.publicActionContext
//        context.elements.entries.removeIf { entry ->
//            if (entry.key.contains(ACTION_DISPLAY) && entry.key.contains(player.uuid.toString())) {
//                entry.value.remove()
//                true
//            } else {
//                false
//            }
//        }
//        displayUUIDCache[player.uuid]?.forEach { uuid ->
//            ActionStorage.contextStorage.remove(uuid) //少しhackyかもしれない
//        }
//        displayUUIDCache.remove(player.uuid)
//    }
//
//    fun discardTile(tile: Tile, context: IActionContext) {
//        val player = context.target ?: return
//        val instance = players.firstOrNull { it.uuid == player.uniqueId } ?: return
//        if (player.uniqueId != turnPlayer.uuid) return
//        discardTile(tile, instance)
//    }
//
//    // 非同期、同期、どちらでも呼び出される可能性がある
//    fun discardTile(tile: Tile, instance: PlayerInstance) {
//        if (turn != instance.position) return
//        if (!instance.tiles.contains(tile)) return
//        val index = instance.tiles.indexOf(tile)
//        if (index == -1) return
//        if (!allowedInteract.compareAndSet(true, false)) return
//
//        val beforeTiles = instance.tiles.toList()
//        val isLastTile = index == instance.tiles.size - 1
//
//        instance.tiles.remove(tile)
//        instance.discard.add(tile)
//
//        val reachTile = instance.discard.indexOfFirst { it.reachTile }
//
//        players.forEach { player ->
//            val samePlayer = player.uuid == instance.uuid
//            val costume = player.mahjongTableCostume
//            val tileCostume = player.tileCostume
//
//            val targetLocation = costume.getDiscardLocation(
//                this.location,
//                instance.discard.size - 1,
//                instance.position
//            )
//
//            if (instance.isReach) {
//                var previous = 0
//                for (element in costume.getDiscardSplit()) {
//                    val range = previous..element
//                    if (reachTile in range && instance.discard.size-1 in range) {
//                        targetLocation.add(costume.getDiscardRichiOffset())
//                        break
//                    }
//                    previous = element
//                }
//            }
//
//            val otherPlayerDisplayTiles = player.otherPlayerDisplayTiles.getOrPut(instance.uuid) { mutableListOf() }
//            val beforeOtherPlayerDisplayTiles = otherPlayerDisplayTiles.toList()
//            val discardTile = if (player.uuid == instance.uuid) {
//                tile
//            } else {
//                val discardTile = if (isLastTile) {
//                    otherPlayerDisplayTiles.removeLast()
//                } else {
//                    otherPlayerDisplayTiles.removeAt(Random.nextInt(otherPlayerDisplayTiles.size - 1)) //最後の牌はツモ牌で使うので除外
//                }
//
//                player.otherPlayerDisplayDiscard.getOrPut(instance.uuid) { mutableListOf() }.add(discardTile)
//                discardTile.copyTileInfo(tile)
//                discardTile
//            }
//
//            if (!samePlayer) {
//                MahjongMc.actionRunner.run(
//                    MahjongMc.emptyAdvancedConfiguration(),
//                    tileCostume.getOnRender(),
//                    MahjongMc.createActionContext(table.parameterizeContext()).apply {
//                        this.location = targetLocation
//                        this.target = player.getPlayer()
//                        prepareParameters.let {
//                            it["tile"] = discardTile.parameters()
//                            it["display"] = TILE_DISPLAY.appendSuffix(discardTile.uuid.toString())
//                        }
//                    },
//                    null,
//                    true,
//                    false
//                )
//            }
//
//            MahjongMc.actionRunner.run(
//                MahjongMc.emptyAdvancedConfiguration(),
//                costume.getOnDiscard(),
//                MahjongMc.createActionContext(table.parameterizeContext()).apply {
//                    this.location = targetLocation
//                    this.target = player.getPlayer()
//                    prepareParameters.let {
//                        it["tile"] = discardTile.parameters()
//                        it["display"] = TILE_DISPLAY.appendSuffix(discardTile.uuid.toString())
//                        it["isRichi"] = false //TODO: 立直牌かどうかの実装
//                        it["hidden"] = player.uuid != instance.uuid
//                    }
//                },
//                null,
//                true,
//                false
//            ).thenRun {
//                table.sortTiles(
//                    instance,
//                    player,
//                    if (samePlayer) beforeTiles else beforeOtherPlayerDisplayTiles
//                )
//            }
//        }
//
//        prepareActions.set(true)
//    }
//}