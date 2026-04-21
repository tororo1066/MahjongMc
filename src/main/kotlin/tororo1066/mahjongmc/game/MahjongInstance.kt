package tororo1066.mahjongmc.game

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bukkit.Location
import tororo1066.displaymonitorapi.configuration.IAdvancedConfigurationSection
import tororo1066.mahjongmc.*
import tororo1066.mahjongmc.costume.MahjongTableCostume
import tororo1066.mahjongmc.enums.PlayerSettings
import tororo1066.mahjongmc.enums.Position
import tororo1066.mahjongmc.game.ui.MahjongDisplays.ACTION_DISPLAY
import tororo1066.mahjongmc.game.ui.MahjongDisplays.CHI_DISPLAY
import tororo1066.mahjongmc.game.ui.MahjongDisplays.CHOICE_DISPLAY
import tororo1066.mahjongmc.game.ui.MahjongDisplays.KAN_DISPLAY
import tororo1066.mahjongmc.game.ui.MahjongDisplays.PON_DISPLAY
import tororo1066.mahjongmc.game.ui.MahjongDisplays.POSITION_DISPLAY
import tororo1066.mahjongmc.game.ui.MahjongDisplays.RICHI_DISPLAY
import tororo1066.mahjongmc.game.ui.MahjongDisplays.RICHI_STICK_DISPLAY
import tororo1066.mahjongmc.game.ui.MahjongDisplays.RON_DISPLAY
import tororo1066.mahjongmc.game.ui.MahjongDisplays.RYUKYOKU_ACTION_DISPLAY
import tororo1066.mahjongmc.game.ui.MahjongDisplays.SKIP_DISPLAY
import tororo1066.mahjongmc.game.ui.MahjongDisplays.TILE_DISPLAY
import tororo1066.mahjongmc.game.ui.MahjongDisplays.TIME_DISPLAY
import tororo1066.mahjongmc.game.ui.MahjongDisplays.TSUMO_DISPLAY
import tororo1066.mahjongmc.game.ui.MahjongRenderer
import tororo1066.mahjongmc.mahjong.Winning
import tororo1066.mahjongmc.mahjong.WinningManager
import tororo1066.mahjongmc.mahjong.WinningType
import tororo1066.mahjongmc.mahjong.yaku.other.DiscardTerminalsAndHonorsThroughout
import tororo1066.mahjongmc.tile.HonorTiles
import tororo1066.mahjongmc.tile.Tile
import tororo1066.mahjongmc.tile.TileType
import tororo1066.tororopluginapi.SJavaPlugin
import kotlin.coroutines.cancellation.CancellationException

open class MahjongInstance(
    val centerLocation: Location,
    val settings: MahjongSettings = MahjongSettings()
) {

    val scope = CoroutineScope(SupervisorJob() + SJavaPlugin.plugin.asyncDispatcher)
    val minecraftScope = CoroutineScope(SupervisorJob() + SJavaPlugin.plugin.minecraftDispatcher)

    val renderer = MahjongRenderer(this)

    // プレイヤーリスト
    val players = mutableListOf<PlayerInstance>()
    // 牌リスト
    val generalTiles = mutableListOf<Tile>()
    // 牌山
    val deadWall = DeadWall()

    var turn: Position = Position.EAST
    lateinit var turnPlayer: PlayerInstance

    var richiSticks: Int = 0

    var round: Int = 1
    var roundWind: Position = Position.EAST
    var dealerPosition: Position = Position.EAST
    var continueCount: Int = 0

    var currentPhase: Phase = Phase.Init

    val selfActed = MutableStateFlow(false)
    val openBonusTile = MutableStateFlow(false)
    val drawReplacementTile = MutableStateFlow(false)

    val kuigaeTiles = mutableListOf<Tile>()

    val richiContext = MutableStateFlow(RichiContext())
    val selfTileActionGate = SelfTileActionGate()

    data class RichiContext(
        val richiableTiles: Set<Tile> = emptySet(),
        val richiDeclared: Boolean = false,
        val richiDiscardedPlayer: PlayerInstance? = null,
        val isDoubleRichi: Boolean = false
    )

    class SelfTileActionGate {
        private val mutex = Mutex()
        private var acted: Boolean = false

        suspend fun tryAct(action: suspend () -> Boolean) {
            mutex.withLock {
                if (acted) return
                val result = action()
                if (result) {
                    acted = true
                }
            }
        }

        fun reset() {
            acted = false
        }
    }

    sealed class Phase {
        abstract suspend fun step(instance: MahjongInstance): Phase?

        object Init: Phase() {
            override suspend fun step(instance: MahjongInstance): Phase {
                instance.shufflePlayers()
                instance.initPlayers()
                instance.renderer.spawnTable()
                return Prepare
            }
        }

        object Prepare : Phase() {
            override suspend fun step(instance: MahjongInstance): Phase {
                instance.prepare()
                return CheckCurrentState
            }
        }

        object CheckCurrentState : Phase() {
            override suspend fun step(instance: MahjongInstance): Phase {
                val currentState = instance.checkCurrentState()
                return currentState ?: Draw
            }
        }

        object Draw : Phase() {
            override suspend fun step(instance: MahjongInstance): Phase {
                instance.drawTile()
                return DiscardWait
            }
        }

        object DiscardWait : Phase() {
            override suspend fun step(instance: MahjongInstance): Phase {
                val next = instance.waitDiscardAction(isCalled = false)
                return next
            }
        }

        object CallDiscardWait : Phase() {
            override suspend fun step(instance: MahjongInstance): Phase {
                val next = instance.waitDiscardAction(isCalled = true)
                return next
            }
        }

        object ToTileActionWait : Phase() {
            override suspend fun step(instance: MahjongInstance): Phase {
                val next = instance.checkToTileAction()
                return next
            }
        }

        object NextTurn : Phase() {
            override suspend fun step(instance: MahjongInstance): Phase {
                instance.nextTurn()
                return CheckCurrentState
            }
        }

        object NextRound : Phase() {
            override suspend fun step(instance: MahjongInstance): Phase {
                instance.players.forEach { player ->
                    instance.renderer.removeDisplays(player, TILE_DISPLAY)
                    instance.renderer.removeDisplays(player, POSITION_DISPLAY)
                    instance.renderer.removeDisplays(player, RICHI_STICK_DISPLAY)
                }
                instance.playersNextRound()
                return Prepare
            }
        }

        object End : Phase() {
            override suspend fun step(instance: MahjongInstance): Phase? {
                instance.end()
                return null
            }
        }
    }

    fun initPlayers() {
        players.forEach {
            it.initCostumes(this)
            it.initRound()
        }
    }

    fun playersNextRound() {
        players.forEach { player ->
            player.initRound()
        }
    }


    fun shufflePlayers() {
        players.shuffle()
        players.forEachIndexed { index, playerInstance ->
            playerInstance.position = Position.entries[index]

            if (playerInstance.position == Position.EAST) {
                setTurnPlayer(playerInstance)
            }
        }
    }

    suspend fun prepare() {
        richiContext.emit(RichiContext())

        setupTiles()
        setTurnPlayer(players.first { it.position == dealerPosition })

        renderer.asyncRenderPosition().join()
        renderer.asyncRenderState().join()
        renderer.spawnDeadWallTiles()
        spawnPrepareTiles()
        delay(12.ticks)
    }

    suspend fun end() {
        renderer.removeAllDisplays()
    }

    fun stop() {
        scope.launch {
            end()
        }.invokeOnCompletion {
            scope.cancel()
            minecraftScope.cancel()
        }
    }

    fun setupTiles() {
        generalTiles.clear()
        for (type in TileType.entries.filter { it != TileType.HONORS }) {
            for (number in 1..9) {
                for (count in 0 until 4) {
                    if (settings.playerSettings == PlayerSettings.PLAYERS_3 && type == TileType.CHARACTERS && number in 2..8) {
                        continue
                    }
                    val tile = Tile(
                        type = type,
                        number = number,
                        isRed = number == 5 && count == 0
                    )

                    generalTiles.add(tile)
                }
            }
        }

        repeat(4) {
            for (honorType in 0 until 7) {
                val tile = Tile(
                    type = TileType.HONORS,
                    number = 0,
                    honor = HonorTiles.entries[honorType]
                )
                generalTiles.add(tile)
            }
        }

        generalTiles.shuffle()

        players.forEach { instance ->
            repeat(13) { i ->
                val existingTile = instance.debugPrepareTiles.getOrNull(i)
                if (existingTile != null) {
                    val tile = generalTiles.find { it.isSameTile(existingTile)  }
                    if (tile != null) {
                        generalTiles.remove(tile)
                        instance.tiles.add(tile)
                        return@repeat
                    }
                }
                val tile = generalTiles.removeFirst()
                instance.tiles.add(tile)
            }
        }

        deadWall.setUpDeadWall(generalTiles, settings.playerSettings)
    }

    fun asyncSpawnTile(player: PlayerInstance, tile: Tile, index: Int, lastTile: Boolean = false): Job {
        return renderer.asyncSpawnTile(
            tile = tile,
            index = index,
            player = player,
            lastTile = lastTile,
            onInteract = { context ->
                if (context.target?.uniqueId != player.uuid) return@asyncSpawnTile
                discardTileByInteraction(player, tile)
            },
            onHover = { context ->
                if (context.target?.uniqueId != player.uuid) return@asyncSpawnTile
                if (!player.tiles.contains(tile)) return@asyncSpawnTile
                if (selfActed.value) return@asyncSpawnTile
                tile.hovered = true
                renderer.asyncHoverTile(player, tile)
            },
            onUnhover = { context ->
                if (context.target?.uniqueId != player.uuid) return@asyncSpawnTile
                if (!player.tiles.contains(tile)) return@asyncSpawnTile
                if (!tile.hovered) return@asyncSpawnTile
                tile.hovered = false
                renderer.asyncUnhoverTile(player, tile)
            }
        )
    }

    suspend fun spawnPrepareTiles(range: IntRange) {
        val tasks = mutableListOf<Job>()

        players.forEach { player ->
            range.forEach { index ->
                val tile = player.tiles[index]
                tasks.add(asyncSpawnTile(player, tile, index))
            }
        }

        tasks.joinAll()
    }

    suspend fun spawnPrepareTiles() {
        spawnPrepareTiles(0..3)

        delay(6.ticks)

        spawnPrepareTiles(4..7)

        delay(6.ticks)

        spawnPrepareTiles(8..11)

        delay(6.ticks)

        spawnPrepareTiles(12..12)

        players.forEach { player ->
            val beforeTiles = player.tiles.toList()
            sortTiles(player)
            renderer.sortTilesAnimation(
                player = player,
                receiver = player,
                beforeTiles = beforeTiles,
            )
        }
    }

    suspend fun checkCurrentState(): Phase? {
        val kanCounts = players.map {
            it.calls.count { call -> call.isKan() }
        }.filter { it > 0 }

        val totalKanCount = kanCounts.sum()
        if (totalKanCount >= 4 && kanCounts.size != 1) {
            return ryukyoku(type = RyukyokuType.MeldedFourFours)
        }

        if (generalTiles.isEmpty()) {
            return ryukyoku()
        }

        return null
    }

    //TODO: 流し満貫のデバッグ
    suspend fun ryukyoku(
        type: RyukyokuType? = null
    ): Phase {
        val beforeScores = players.associateWith { it.score }

        val tenpaiPlayers = players.filter { WinningManager.isTenpai(it.tiles, it.calls) }
        val nonTenpaiPlayers = players - tenpaiPlayers.toSet()

        val nagashiManganPlayers = mutableListOf<PlayerInstance>()
        val nagashiManganScores = mutableMapOf<PlayerInstance, Int>()

        if (type == null) {
            players.forEach { player ->
                // 捨て牌が全て么九牌であり、かつ副露されていない場合流し満貫
                if (player.discard.all { it.type == TileType.HONORS || it.number == 1 || it.number == 9 }
                    && players.none { it.calls.any { call -> call.target == player.position } }) {
                    nagashiManganPlayers.add(player)
                }
            }

            if (nagashiManganPlayers.isNotEmpty()) {
                nagashiManganPlayers.forEach { player ->
                    val score = WinningManager.paymentNagashiManganWinning(
                        instance = this,
                        player = player
                    )
                    nagashiManganScores[player] = score
                }
            } else {
                if (tenpaiPlayers.isNotEmpty() && nonTenpaiPlayers.isNotEmpty()) {
                    val totalPayment = (settings.playerSettings.seats - 1) * 1000
                    val payment = totalPayment / nonTenpaiPlayers.size
                    val reward = totalPayment / tenpaiPlayers.size

                    tenpaiPlayers.forEach { player ->
                        player.score += reward
                    }

                    nonTenpaiPlayers.forEach { player ->
                        player.score -= payment
                    }
                }
            }

            val tenpaiTiles = tenpaiPlayers.associateWith { player ->
                WinningManager.getTenpaiTiles(
                    tiles = player.tiles,
                    calls = player.calls
                ).toList()
            }

            renderer.renderRyukyoku(
                type = RyukyokuType.Normal(tenpaiTiles = tenpaiTiles)
            )
        } else {
            renderer.renderRyukyoku(
                type = type
            )
        }

        nagashiManganPlayers.forEach { player ->
            renderer.renderWinning(
                player = player,
                winnings = listOf(DiscardTerminalsAndHonorsThroughout),
                winningTile = null,
                fu = null,
                score = nagashiManganScores[player] ?: 0,
                winningType = WinningType.NORMAL,
                winningBy = MahjongRenderer.WinningBy.NAGASHI_MANGAN,
            ) { null }
        }

        renderer.renderScoreChange(
            beforeScores = beforeScores
        )

        if (type == null) {
            val nextRound = nextRound(
                continuablePlayers = tenpaiPlayers + nagashiManganPlayers,
                isRyukyoku = true
            )

            if (nextRound == Phase.End) {
                players.maxBy {
                    it.score
                }.score += richiSticks * 1000
            }

            return nextRound
        } else {
            return Phase.NextRound
        }
    }

    suspend fun drawTile() {
        val turnPlayer = turnPlayer

        val drawReplacementTile = drawReplacementTile.compareAndSet(expect = true, update = false)

        val tile = if (drawReplacementTile) {
            deadWall.takeReplacementTile(generalTiles)
        } else {
            generalTiles.removeFirstOrNull()
        } ?: return

        turnPlayer.tiles.add(tile)

        renderer.asyncRenderState()

        asyncSpawnTile(turnPlayer, tile, turnPlayer.tiles.size - 1, lastTile = true).join()
    }

    fun timeCountdown(
        player: PlayerInstance,
        onTimeout: suspend () -> Unit
    ): Job {
        var holdTime = player.holdTime
        var backupTime = player.backupTime
        val totalTime = holdTime + backupTime
        var ticks = (totalTime) * 20

        return scope.launch {
            try {
                while (true) {
                    if (ticks <= 0) {
                        renderer.removeDisplays(player, TIME_DISPLAY)
                        onTimeout()
                        break
                    }

                    if (ticks % 20 == 0) {
                        renderer.renderTimeDisplay(
                            player = player,
                            currentTime = ticks / 20,
                            holdTime = holdTime,
                            backupTime = backupTime
                        )

                        if (backupTime <= 0) {
                            holdTime--
                        } else {
                            backupTime--
                        }
                    }

                    delay(1.ticks)
                    ticks--
                }
            } catch (e: CancellationException) {
                withContext(NonCancellable) {
                    renderer.removeDisplays(player, TIME_DISPLAY)
                }
                throw e
            } finally {
                player.setNextTimes(holdTime, backupTime)
            }
        }
    }

    fun discardTileByInteraction(player: PlayerInstance, tile: Tile) {
        if (player.isRichi) return
        scope.launch {
            discardTile(player, tile)
        }
    }

    //TODO: 時間経過での捨て問題
    suspend fun discardTile(player: PlayerInstance, tile: Tile, force: Boolean = false): Boolean {
        suspend fun func(): Boolean {
            if (currentPhase != Phase.DiscardWait) return false
            if (turn != player.position) return false
            if (!player.tiles.contains(tile)) return false
            if (!settings.allowKuigae && kuigaeTiles.contains(tile)) return false
            val index = player.tiles.indexOf(tile)
            if (index == -1) return false
//            if (currentPhase != Phase.DiscardWait && currentPhase != Phase.CallDiscardWait) {
//                SJavaPlugin.plugin.logger.warning("Cannot discard tile because current phase is not DiscardWait")
//                return false
//            }
//            if (turn != player.position) {
//                SJavaPlugin.plugin.logger.warning("Cannot discard tile because it's not player's turn")
//                return false
//            }
//            if (!player.tiles.contains(tile)) {
//                SJavaPlugin.plugin.logger.warning("Cannot discard tile because player does not have the tile")
//                return false
//            }
//            if (!settings.allowKuigae && kuigaeTiles.contains(tile)) {
//                SJavaPlugin.plugin.logger.warning("Cannot discard tile because it's a kuigae tile")
//                return false
//            }
//            val index = player.tiles.indexOf(tile)
//            if (index == -1) {
//                SJavaPlugin.plugin.logger.warning("Cannot discard tile because tile index is invalid")
//                return false
//            }

            val richiContextValue = richiContext.value

            val richiDeclared = richiContextValue.richiDeclared

            if (richiDeclared) {
                if (!richiContextValue.richiableTiles.contains(tile)) {
//                    SJavaPlugin.plugin.logger.warning("Cannot discard tile because it's not a richiable tile")
                    return false
                }
            }

            val beforeTiles = player.tiles.toList()
            val isLastTile = index == player.tiles.size - 1
            val richiTile = player.discard.indexOfFirst { it.isRichiTile }

            // 立直後の牌が副露によって取られた場合次の牌を横にする
            if (richiDeclared || (player.isRichi && richiTile == -1)) {
                tile.isRichiTile = true
            }

            player.tiles.removeAt(index)
            player.discard.add(tile)

            sortTiles(player)

            player.isOneShot = false

            if (richiDeclared) {
                val isDoubleRichi = player.discard.size == 1 && players.all { it.calls.isEmpty() }

                richiContext.update {
                    it.copy(
                        richiableTiles = emptySet(),
                        richiDeclared = false,
                        richiDiscardedPlayer = player,
                        isDoubleRichi = isDoubleRichi
                    )
                }

                renderer.asyncRenderRichi(
                    player = player,
                    isDoubleRichi = isDoubleRichi
                )
            }

            renderer.renderDiscardTile(
                player = player,
                tile = tile,
                isLastTile = isLastTile,
                richiTile = richiTile,
                beforeTiles = beforeTiles
            )

            if (openBonusTile.compareAndSet(expect = true, update = false)) {
                revealBonusTile()
            }

            clearTilesState(player)

            selfActed.update { true }

            return true
        }

        if (force) {
            return func()
        } else {
            var result = false
            selfTileActionGate.tryAct {
                result = func()
                result
            }
            return result
        }
    }

    // ドラ表示牌をめくる
    fun revealBonusTile(): Job? {
        val tile = deadWall.openBonusTile(settings.playerSettings) ?: return null
        return renderer.renderRevealBonusTile(tile)
    }

    fun sortTiles(player: PlayerInstance) {
        player.tiles.sortWith(compareBy({ it.type.ordinal }, { it.number }, { it.honor.ordinal }))
    }

    suspend fun callTiles(
        call: Call,
        caller: PlayerInstance,
        discardPlayer: PlayerInstance,
        discardTile: Tile?
    ): Phase {
        discardPlayer.discard.remove(discardTile)
        val beforeTiles = caller.tiles.toList()

        var call = call
        var step = caller.calls.size
        if (call.type == Call.Type.LATE_KAN) {
            // 加槓の場合既存のポンを槓に変える
            val existingCall = caller.calls.firstOrNull { existingCall ->
                existingCall.type == Call.Type.PON &&
                        existingCall.tiles.all { call.tiles.contains(it) }
            } ?: throw IllegalStateException("Late kan call but no existing pon found")
            val index = caller.tiles.indexOfFirst { tile -> tile.isSameTile(call.calledTile) }
            if (index == -1) throw IllegalStateException("Late kan call but called tile not found in caller's tiles")
            val tile = caller.tiles.removeAt(index)
            existingCall.type = Call.Type.LATE_KAN
            existingCall.tiles.add(tile)
            call = existingCall
            step = caller.calls.indexOf(existingCall) + 1
        } else {
            caller.tiles.removeAll(call.tiles.filter { it != discardTile })
            sortTiles(caller)
            caller.calls.add(call)
        }

        renderer.callTiles(
            caller = caller,
            discardPlayer = discardPlayer,
            call = call,
            step = step,
            beforeTiles = beforeTiles,
            discardTile = discardTile
        )

        if (call.isKan()) {
            drawReplacementTile.update { true }

            if (call.type == Call.Type.CONCEALED_KAN) {
                revealBonusTile()
            } else {
                openBonusTile.update { true }
            }

            if (call.type != Call.Type.KAN) {
                call.tiles.forEach { it.isSelfKanTile = true }
                checkChankan(call)?.let { return it }
            }
        }

        players.forEach { player -> player.isOneShot = false }

        if (call.type == Call.Type.PON || call.type == Call.Type.CHI) {
            checkKuigaeTiles(caller, call)
        }

        setTurnPlayer(caller)

        return if (call.isKan()) {
            Phase.Draw
        } else {
            Phase.CallDiscardWait
        }
    }

    // 牌を引いた後のアクション待ち
    suspend fun waitDiscardAction(isCalled: Boolean): Phase {

        val richiContextValue = richiContext.value
        val richiDiscardedPlayer = richiContextValue.richiDiscardedPlayer
        if (richiDiscardedPlayer != null) {
            richiDiscardedPlayer.isRichi = true
            richiDiscardedPlayer.isDoubleRichi = richiContextValue.isDoubleRichi
            richiDiscardedPlayer.isOneShot = true
            richiDiscardedPlayer.score -= 1000
            richiSticks++

            if (players.all { it.isRichi } && players.size >= 4) {
                return ryukyoku(type = RyukyokuType.FourPlayersRichi)
            }

            renderer.asyncRenderRichiStick(
                player = richiDiscardedPlayer
            )
        }

        val turnPlayer = turnPlayer

        // 同巡内フリテンの解消
        turnPlayer.ignoredRonOnSilent = false

        val canConcealedKan = !isCalled && turnPlayer.canConcealedKan(this)
        val canLateKan = !isCalled && turnPlayer.canLateKan(this)
        val canRichi = !isCalled && turnPlayer.canRichi(this)
        val canTsumo = !isCalled && turnPlayer.canTsumo(this) // TODO: 天和時の和了牌の自動選択
        val canRyukyoku = !isCalled && turnPlayer.canNineDifferentTerminalsRyukyoku(this)

        selfActed.emit(false)

        if (turnPlayer.isRichi && (!canConcealedKan && !canLateKan && !canTsumo)) {
            // 立直後に何もできない場合は強制的に捨てる
            val result = discardTile(
                turnPlayer,
                turnPlayer.tiles.last(),
                force = true
            )
            if (!result) throw IllegalStateException("Failed to discard tile for richi player with no actions")
            return Phase.ToTileActionWait
        }

        richiContext.emit(RichiContext())
        selfTileActionGate.reset()

        var actedAction: Pair<SelfTileAction, Call?>? = null

        val timeCountdownJob = timeCountdown(
            player = turnPlayer,
            onTimeout = {
                actedAction = null
                val result = discardTile(
                    turnPlayer,
                    turnPlayer.tiles.last()
                )
                if (!result) throw IllegalStateException("Failed to discard tile on timeout")
            }
        )

        if (canConcealedKan || canLateKan || canRichi || canTsumo || canRyukyoku) {
            val mahjongTableCostume = turnPlayer.getCostume<MahjongTableCostume>()
            var concealed = false
            var index = 0
            val displays = listOfNotNull(
                SKIP_DISPLAY,
                if (canConcealedKan || canLateKan) KAN_DISPLAY else null,
                if (canRichi) RICHI_DISPLAY else null,
                if (canTsumo) TSUMO_DISPLAY else null,
                if (canRyukyoku) RYUKYOKU_ACTION_DISPLAY else null,
            )

            fun concealDisplays() {
                if (concealed) return
                concealed = true
                renderer.asyncConcealDisplays(
                    player = turnPlayer,
                    displays = displays
                )
            }

            fun revealDisplays() {
                if (!concealed) return
                concealed = false
                renderer.asyncRevealDisplays(
                    player = turnPlayer,
                    displays = displays
                )
            }

            fun spawnActionDisplay(
                name: String,
                displayActions: List<IAdvancedConfigurationSection>,
                onInteract: () -> Unit
            ) {
                renderer.asyncSpawnActionDisplay(
                    player = turnPlayer,
                    name = name,
                    index = index++,
                    actions = displayActions
                ) { _ ->
                    if (concealed) return@asyncSpawnActionDisplay
                    if (actedAction != null) return@asyncSpawnActionDisplay
                    onInteract()
                }
            }

            spawnActionDisplay(
                name = SKIP_DISPLAY,
                displayActions = mahjongTableCostume.getSkipDisplay()
            ) {
                renderer.asyncRemoveDisplays(turnPlayer, ACTION_DISPLAY)
                actedAction = SelfTileAction.SKIP to null
                // 立直中は捨てる動作をここで行う
                if (turnPlayer.isRichi) {
                    scope.launch {
                        discardTile(
                            turnPlayer,
                            turnPlayer.tiles.last()
                        )
                    }
                }
            }

            if (canConcealedKan || canLateKan) {
                spawnActionDisplay(
                    name = KAN_DISPLAY,
                    displayActions = mahjongTableCostume.getKanDisplay()
                ) {
                    val kanCalls = mutableListOf<Call>()
                    if (canConcealedKan) {
                        kanCalls.addAll(turnPlayer.concealedKanChoices().map { callTiles ->
                            Call(Call.Type.CONCEALED_KAN, callTiles, turnPlayer.position, callTiles.first())
                        })
                    }
                    if (canLateKan) {
                        kanCalls.addAll(turnPlayer.lateKanChoices().map { callTiles ->
                            Call(Call.Type.LATE_KAN, callTiles, turnPlayer.position, callTiles.last())
                        })
                    }

                    fun onChosen(call: Call) {
                        actedAction = SelfTileAction.KAN to call
                        scope.launch {
                            selfTileActionGate.tryAct {
                                selfActed.update { true }
                                renderer.asyncRemoveDisplays(turnPlayer, ACTION_DISPLAY)
                                true
                            }
                        }
                    }

                    if (kanCalls.size == 1) {
                        onChosen(kanCalls.first())
                    } else {
                        concealDisplays()
                        renderer.renderChoiceCallDisplays(
                            player = turnPlayer,
                            possibleCalls = kanCalls,
                            onChosen = ::onChosen,
                            onCanceled = {
                                renderer.asyncRemoveDisplays(turnPlayer, CHOICE_DISPLAY)
                                revealDisplays()
                            }
                        )
                    }
                }
            }

            if (canRichi) {
                spawnActionDisplay(
                    name = RICHI_DISPLAY,
                    displayActions = mahjongTableCostume.getRichiDisplay()
                ) {
                    val richiDiscards = WinningManager.richiDiscards(
                        tiles = turnPlayer.tiles,
                        calls = turnPlayer.calls
                    )
                    if (richiDiscards.isEmpty()) {
                        return@spawnActionDisplay
                    }

//                    actedAction = SelfTileAction.RICHI to null

                    concealDisplays()

                    scope.launch {
                        selfTileActionGate.tryAct {
                            richiContext.update {
                                it.copy(
                                    richiableTiles = richiDiscards.toSet(),
                                    richiDeclared = true
                                )
                            }

                            renderer.asyncRenderRichiableTiles(
                                player = turnPlayer,
                                richiableTiles = richiDiscards.toList(),
                                notRichiableTiles = turnPlayer.tiles - richiDiscards
                            ) {
                                if (!concealed) return@asyncRenderRichiableTiles
                                scope.launch {
                                    selfTileActionGate.tryAct {
                                        richiContext.update {
                                            it.copy(
                                                richiableTiles = emptySet(),
                                                richiDeclared = false
                                            )
                                        }
                                        renderer.asyncRenderRemoveStateTiles(turnPlayer, turnPlayer.tiles).invokeOnCompletion {
                                            renderer.asyncRenderKuigaeTiles(turnPlayer, kuigaeTiles)
                                        }
                                        revealDisplays()
                                        false // こっちも
                                    }
                                }
                            }
                            false // 立直宣言後の捨て牌はdiscardTile内で処理するためここではfalseを返す
                        }
                    }
                }
            }

            if (canTsumo) {
                spawnActionDisplay(
                    name = TSUMO_DISPLAY,
                    displayActions = mahjongTableCostume.getTsumoDisplay()
                ) {
                    scope.launch {
                        selfTileActionGate.tryAct {
                            actedAction = SelfTileAction.TSUMO to null
                            selfActed.update { true }
                            true
                        }
                    }
                    renderer.asyncRemoveDisplays(turnPlayer, ACTION_DISPLAY)
                }
            }

            if (canRyukyoku) {
                spawnActionDisplay(
                    name = RYUKYOKU_ACTION_DISPLAY,
                    displayActions = mahjongTableCostume.getRyukyokuActionDisplay()
                ) {
                    scope.launch {
                        selfTileActionGate.tryAct {
                            actedAction = SelfTileAction.RYUKYOKU to null
                            selfActed.update { true }
                            true
                        }
                    }
                    renderer.asyncRemoveDisplays(turnPlayer, ACTION_DISPLAY)
                }
            }
        }

        selfActed.first { it }
        timeCountdownJob.cancel()
        renderer.asyncRemoveDisplays(turnPlayer, ACTION_DISPLAY)

        actedAction?.let { (actionType, call) ->
            when (actionType) {
                SelfTileAction.KAN -> {
                    val call = call!!
                    return callTiles(
                        call = call,
                        caller = turnPlayer,
                        discardPlayer = turnPlayer,
                        discardTile = null
                    )
                }
                SelfTileAction.TSUMO -> {
                    val winningTile = turnPlayer.tiles.last()
                    val nextPhase = tsumo(
                        player = turnPlayer,
                        winningTile = winningTile
                    )
                    return nextPhase
                }
                SelfTileAction.RYUKYOKU -> {
                    return ryukyoku(
                        type = RyukyokuType.NineDifferentTerminals(player = turnPlayer)
                    )
                }
                else -> {}
            }
        } ?: run {
            if (players.all { it.calls.isEmpty() }) {
                val tile = turnPlayer.discard.firstOrNull()
                if (tile != null && tile.isWind()) {
                    if (players.all { player ->
                        player.discard.size == 1 && player.discard.first().isSameTile(tile)
                    }) {
                        return ryukyoku(type = RyukyokuType.DiscardingTheSameWind)
                    }
                }
            }
        }

        return Phase.ToTileActionWait
    }

    suspend fun checkChankan(call: Call): Phase? {
        val discardTile = call.calledTile
        val ronPlayers = players.filter { player ->
            if (player == turnPlayer) return@filter false
            if (!player.canRon(discardTile, this)) return@filter false
            if (call.type == Call.Type.CONCEALED_KAN) {
                return@filter WinningManager.checkThirteenOrphans(
                    tiles = player.tiles + discardTile
                )
            }
            return@filter true
        }

        if (ronPlayers.isEmpty()) {
            return null
        }

        val actedPlayers = mutableMapOf<PlayerInstance, ToTileAction>()
        val continueCheck = MutableStateFlow(true)

        val timeCountdownJobs = mutableListOf<Job>()

        ronPlayers.forEach { player ->
            val mahjongTableCostume = player.getCostume<MahjongTableCostume>()

            val timeCountdownJob = timeCountdown(
                player = player,
                onTimeout = {
                    if (actedPlayers.containsKey(player)) return@timeCountdown
                    actedPlayers[player] = ToTileAction.SKIP
                    continueCheck.value = actedPlayers.size < ronPlayers.size
                    renderer.asyncRemoveDisplays(player, ACTION_DISPLAY)
                }
            )

            timeCountdownJobs.add(timeCountdownJob)

            renderer.asyncSpawnActionDisplay(
                player = player,
                name = SKIP_DISPLAY,
                index = 0,
                actions = mahjongTableCostume.getRonDisplay()
            ) { _ ->
                if (actedPlayers.containsKey(player)) return@asyncSpawnActionDisplay
                actedPlayers[player] = ToTileAction.SKIP
                continueCheck.value = actedPlayers.size < ronPlayers.size
                timeCountdownJob.cancel()
                renderer.asyncRemoveDisplays(player, ACTION_DISPLAY)
            }

            renderer.asyncSpawnActionDisplay(
                player = player,
                name = RON_DISPLAY,
                index = 1,
                actions = mahjongTableCostume.getRonDisplay()
            ) { _ ->
                if (actedPlayers.containsKey(player)) return@asyncSpawnActionDisplay
                actedPlayers[player] = ToTileAction.RON
                continueCheck.value = actedPlayers.size < ronPlayers.size
                timeCountdownJob.cancel()
                renderer.asyncRemoveDisplays(player, ACTION_DISPLAY)
            }
        }

        continueCheck.filter { !it }.first()
        timeCountdownJobs.forEach { it.cancel() }

        val ronPlayersFinal = actedPlayers.filter { it.value == ToTileAction.RON }.keys
        if (ronPlayersFinal.isEmpty()) {
            return null
        }

        return ron(
            ronPlayers = ronPlayersFinal,
            targetPlayer = turnPlayer,
            discardTile = discardTile
        )
    }

    suspend fun checkToTileAction(): Phase {
        val turnPlayer = turnPlayer
        val discardTile = turnPlayer.discard.last()

        val actablePlayers = mutableMapOf<PlayerInstance, MutableList<ToTileAction>>()
        val actedPlayers = mutableMapOf<PlayerInstance, Pair<ToTileAction, Call?>>()

        fun canAnyAct(): Boolean {
            val ronPlayers = actablePlayers.filter { it.value.contains(ToTileAction.RON) }.keys
            if (ronPlayers.isNotEmpty()) {
                return !ronPlayers.all { actedPlayers.containsKey(it) }
            }

            val ponKanPlayer = actablePlayers.entries.find { entry ->
                (entry.value.contains(ToTileAction.PON) ||
                        entry.value.contains(ToTileAction.KAN))
                        && actedPlayers.containsKey(entry.key)
            }

            if (ponKanPlayer != null) {
                return false
            }

            val chiPlayer = actablePlayers.entries.find { entry ->
                entry.value.contains(ToTileAction.CHI)
                        && actedPlayers.containsKey(entry.key)
            }

            return chiPlayer == null
        }

        val continueCheck = MutableStateFlow(true)
        val timeCountdownJobs = mutableListOf<Job>()

        players.forEach { player ->
            if (player == turnPlayer) return@forEach
            val mahjongTableCostume = player.getCostume<MahjongTableCostume>()

            val canPon = player.canPon(discardTile, this)
            val canChi = player.canChi(discardTile, this, turnPlayer.position)
            val canKan = player.canKan(discardTile, this)
            val canRon = player.canRon(discardTile, this)

            var concealed = false

            if (!canPon && !canChi && !canKan && !canRon) return@forEach

            val displays = listOfNotNull(
                SKIP_DISPLAY,
                if (canPon) PON_DISPLAY else null,
                if (canChi) CHI_DISPLAY else null,
                if (canKan) KAN_DISPLAY else null,
                if (canRon) RON_DISPLAY else null,
            )

            fun concealDisplays() {
                if (concealed) return
                concealed = true
                renderer.asyncConcealDisplays(player, displays)
            }

            fun revealDisplays() {
                if (!concealed) return
                concealed = false
                renderer.asyncRevealDisplays(player, displays)
            }

            var index = 1
            val timeCountdownJob = timeCountdown(
                player = player,
                onTimeout = {
                    if (actedPlayers.containsKey(player)) return@timeCountdown
                    actedPlayers[player] = ToTileAction.SKIP to null
                    continueCheck.value = canAnyAct()
                    renderer.asyncRemoveDisplays(player, ACTION_DISPLAY)
                }
            )

            timeCountdownJobs.add(timeCountdownJob)

            fun spawnCallDisplay(
                name: String,
                actionType: ToTileAction,
                displayActions: List<IAdvancedConfigurationSection>,
                getChoices: () -> List<List<Tile>>,
                createCall: (callTiles: List<Tile>) -> Call
            ) {
                actablePlayers.getOrPut(player) { mutableListOf() }.add(actionType)
                renderer.asyncSpawnActionDisplay(
                    player = player,
                    name = name,
                    index = index++,
                    actions = displayActions
                ) { _ ->
                    if (concealed) return@asyncSpawnActionDisplay
                    if (actedPlayers.containsKey(player)) return@asyncSpawnActionDisplay

                    val choices = getChoices()
                    if (choices.size == 1) {
                        actedPlayers[player] =
                            actionType to createCall(choices.first())

                        continueCheck.value = canAnyAct()
                        timeCountdownJob.cancel()
                        renderer.asyncRemoveDisplays(player, ACTION_DISPLAY)
                    } else {
                        concealDisplays()
                        renderer.renderChoiceCallDisplays(
                            player = player,
                            possibleCalls = choices.map { callTiles ->
                                createCall(callTiles)
                            },
                            onChosen = { call ->
                                if (actedPlayers.containsKey(player)) return@renderChoiceCallDisplays
                                actedPlayers[player] =
                                    actionType to call

                                continueCheck.value = canAnyAct()
                                timeCountdownJob.cancel()
                                renderer.asyncRemoveDisplays(player, ACTION_DISPLAY)
                            },
                            onCanceled = {
                                revealDisplays()
                                renderer.asyncRemoveDisplays(player, CHOICE_DISPLAY)
                            }
                        )
                    }
                }
            }

            renderer.asyncSpawnActionDisplay(
                player = player,
                name = SKIP_DISPLAY,
                index = 0,
                actions = mahjongTableCostume.getSkipDisplay()
            ) { _ ->
                if (concealed) return@asyncSpawnActionDisplay
                if (actedPlayers.containsKey(player)) return@asyncSpawnActionDisplay
                actedPlayers[player] = ToTileAction.SKIP to null
                continueCheck.value = canAnyAct()
                timeCountdownJob.cancel()
                renderer.asyncRemoveDisplays(player, ACTION_DISPLAY)
            }

            if (canPon) {
                spawnCallDisplay(
                    name = PON_DISPLAY,
                    actionType = ToTileAction.PON,
                    displayActions = mahjongTableCostume.getPonDisplay(),
                    getChoices = { player.ponChoices(discardTile) },
                    createCall = { callTiles ->
                        Call(Call.Type.PON, callTiles, turnPlayer.position, discardTile)
                    }
                )
            }

            if (canChi) {
                spawnCallDisplay(
                    name = CHI_DISPLAY,
                    actionType = ToTileAction.CHI,
                    displayActions = mahjongTableCostume.getChiDisplay(),
                    getChoices = { player.chiChoices(discardTile) },
                    createCall = { callTiles ->
                        Call(Call.Type.CHI, callTiles, turnPlayer.position, discardTile)
                    }
                )
            }

            if (canKan) {
                spawnCallDisplay(
                    name = KAN_DISPLAY,
                    actionType = ToTileAction.KAN,
                    displayActions = mahjongTableCostume.getKanDisplay(),
                    getChoices = { player.kanChoices(discardTile) },
                    createCall = { callTiles ->
                        Call(Call.Type.KAN, callTiles, turnPlayer.position, discardTile)
                    }
                )
            }

            if (canRon) {
                actablePlayers.getOrPut(player) { mutableListOf() }.add(ToTileAction.RON)
                renderer.asyncSpawnActionDisplay(
                    index = index++,
                    player = player,
                    name = RON_DISPLAY,
                    actions = mahjongTableCostume.getRonDisplay()
                ) { _ ->
                    if (concealed) return@asyncSpawnActionDisplay
                    if (actedPlayers.containsKey(player)) return@asyncSpawnActionDisplay
                    actedPlayers[player] = ToTileAction.RON to null
                    continueCheck.value = canAnyAct()
                    timeCountdownJob.cancel()
                    renderer.asyncRemoveDisplays(player, ACTION_DISPLAY)
                }
            }
        }

        if (actablePlayers.isEmpty()) return Phase.NextTurn

        continueCheck.filter { !it }.first()
        timeCountdownJobs.forEach { it.cancel() }

        players.forEach { player ->
            renderer.removeDisplays(player, ACTION_DISPLAY)
            renderer.removeDisplays(player, CHOICE_DISPLAY)

            if (player == turnPlayer) return@forEach
            val tenpaiTiles = WinningManager.getTenpaiTiles(player.tiles, player.calls)
            if (tenpaiTiles.any { it.isSameTile(discardTile) } && actedPlayers[player]?.first != ToTileAction.RON) {
                if (player.isRichi) {
                    player.ignoredRonOnRichi = true
                } else {
                    player.ignoredRonOnSilent = true
                }
            }
        }

        //ロン > ポン・カン > チー の順で処理
        val ronPlayers = actedPlayers.filter { it.value.first == ToTileAction.RON }
        if (ronPlayers.isNotEmpty()) {
            return ron(
                ronPlayers = ronPlayers.keys,
                targetPlayer = turnPlayer,
                discardTile = discardTile
            )
        } else {
            //ポン、カン、チーは同時にできないので最初に見つけた人だけ処理
            val actionPlayer = actedPlayers.entries.find {
                it.value.first == ToTileAction.PON || it.value.first == ToTileAction.KAN
            } ?: actedPlayers.entries.find {
                it.value.first == ToTileAction.CHI
            }

            if (actionPlayer != null) {
                val (player, action) = actionPlayer
                val (_, call) = action.first to action.second!!
                return callTiles(
                    call = call,
                    caller = player,
                    discardPlayer = turnPlayer,
                    discardTile = discardTile
                )
            }
        }

        return Phase.NextTurn
    }

    suspend fun tsumo(
        player: PlayerInstance,
        winningTile: Tile
    ): Phase {
        val beforeScores = players.associateWith { it.score }

        val winning = WinningManager.findBestWinning(
            instance = this,
            player = player,
            tiles = player.tiles,
            winningTile = winningTile,
            isTsumo = true
        ) ?: throw IllegalStateException("Tsumo called but no winning found")

        val fu = WinningManager.getFu(
            instance = this,
            player = player,
            winningStructure = winning.winningStructure,
            winnings = winning.winnings,
            isTsumo = true
        )

        renderer.renderTsumo(
            player = player,
            winningTile = winningTile
        )

        val visualTotalScore = WinningManager.paymentTsumoWinning(
            instance = this,
            player = player,
            winning = winning,
            richiSticks = richiSticks,
            continueCount = continueCount
        )

        renderer.renderWinning(
            player = player,
            winnings = winning.winnings,
            winningTile = winningTile,
            fu = fu,
            score = visualTotalScore,
            winningType = winning.winningType,
            winningBy = MahjongRenderer.WinningBy.TSUMO
        ) {
            getHan(
                instance = this@MahjongInstance,
                player = player,
                winningStructure = winning.winningStructure,
                isTsumo = true
            )
        }

        renderer.renderScoreChange(beforeScores)
        return nextRound(listOf(player), isRyukyoku = false)
    }

    suspend fun ron(
        ronPlayers: Set<PlayerInstance>,
        targetPlayer: PlayerInstance,
        discardTile: Tile
    ): Phase {
        val beforeScores = players.associateWith { it.score }
        val winningsData = mutableMapOf<PlayerInstance, Pair<Winning, Int>>()

        // 上家から順に処理
        val ronPlayers = ronPlayers.sortedBy {
            (it.position.ordinal - targetPlayer.position.ordinal + settings.playerSettings.seats) % settings.playerSettings.seats
        }

        ronPlayers.forEachIndexed { index, player ->
            val winning = WinningManager.findBestWinning(
                instance = this,
                player = player,
                tiles = player.tiles + discardTile,
                winningTile = discardTile,
                isTsumo = false
            ) ?: return@forEachIndexed

            val score = WinningManager.paymentRonWinning(
                instance = this,
                player = player,
                payer = targetPlayer,
                winning = winning,
                richiSticks = if (index == 0) richiSticks else 0,
                continueCount = if (index == 0) continueCount else 0
            )

            winningsData[player] = winning to score
        }

        winningsData.forEach { (player, data) ->
            val (winning, score) = data
            val (winnings, winningStructure, _, winningType) = winning

            val fu = WinningManager.getFu(
                instance = this,
                player = player,
                winningStructure = winningStructure,
                winnings = winnings,
                isTsumo = false
            )

            renderer.renderRon(
                players = ronPlayers,
                targetPlayer = targetPlayer,
                winningTile = discardTile
            )

            renderer.renderWinning(
                player = player,
                winnings = winnings,
                winningTile = discardTile,
                fu = fu,
                score = score,
                winningType = winningType,
                winningBy = MahjongRenderer.WinningBy.RON
            ) {
                getHan(
                    instance = this@MahjongInstance,
                    player = player,
                    winningStructure = winningStructure,
                    isTsumo = false
                )
            }
        }


        renderer.renderScoreChange(beforeScores)
        return nextRound(ronPlayers, isRyukyoku = false)
    }

    fun checkKuigaeTiles(player: PlayerInstance, call: Call): Job {
        if (settings.allowKuigae) return scope.launch {}
        val kuigaeTiles = player.getKuigaeTiles(call)
        this.kuigaeTiles.addAll(kuigaeTiles)

        return renderer.asyncRenderKuigaeTiles(
            player = player,
            tiles = kuigaeTiles
        )
    }

    fun clearTilesState(player: PlayerInstance): Job {
        kuigaeTiles.clear()
        return renderer.asyncRenderRemoveStateTiles(
            player = player,
            tiles = player.tiles
        )
    }

    fun nextRound(
        continuablePlayers: List<PlayerInstance>,
        isRyukyoku: Boolean
    ): Phase {
        if (players.any { it.score < 0 }) {
            return Phase.End
        }

        // continuablePlayers: 和了したプレイヤー、または流局時に聴牌していたプレイヤー

        val maxScore = players.maxOf { it.score }
        val maxScorePlayers = players.filter { it.score == maxScore }

        val seats = settings.playerSettings.seats

        // 現在の局番号(round)から、これまでに場風が変わった回数を求める（round は 1 始まりで「seats局/1場」）
        val windChangeCount = (round - 1) / seats
        val isLast = windChangeCount == settings.battleType.roundsOfWindChange && round % seats == 0

        // 親が continuablePlayers に含まれる場合は連荘
        val dealerContinues = continuablePlayers.any { it.position == dealerPosition }

        // 終局判定
        val endGame = when {
            // 規定の場の最終場に居て、トップが親+終了点以上で尚且つ連荘する場合は終了
            isLast -> {
                if (maxScore >= settings.endScore) {
                    if (dealerContinues) {
                        maxScorePlayers.size == 1 && maxScorePlayers.first().position == dealerPosition
                    } else {
                        true
                    }
                } else {
                    false
                }
            }

            // 規定場を超えて延長中（= さらに場風が進んだ後）
            windChangeCount > settings.battleType.roundsOfWindChange -> {
                // 1周（= seats局）を超えての延長はしない
                if (round % seats == 0) {
                    true
                } else {
                    // 途中でも終了点に達していれば終了
                    maxScore >= settings.endScore
                }
            }

            else -> false
        }

        if (endGame) {
            return Phase.End
        }

        if (dealerContinues) {
            continueCount++
        } else {
            players.forEach { player ->
                player.position = player.position.next(settings.playerSettings)
            }
            dealerPosition = dealerPosition.next(settings.playerSettings)
            if (!isRyukyoku) {
                continueCount = 0
            }

            round += 1
            if (round % seats == 1) {
                // seats局ごとに場風を進める（東→南→西→北）
                roundWind = roundWind.next(settings.playerSettings)
            }
        }

        if (!isRyukyoku) {
            richiSticks = 0
        }

        return Phase.NextRound
    }

    @JvmName("setTurnPlayerExplicit")
    fun setTurnPlayer(player: PlayerInstance) {
        turn = player.position
        turnPlayer = player
    }

    fun nextTurn() {
        //players.sizeを考慮して回す
        val currentIndex = players.indexOfFirst { it.position == turn }
        val nextIndex = (currentIndex + 1) % players.size
        val nextPlayer = players[nextIndex]
        setTurnPlayer(nextPlayer)
    }



    fun start() = scope.launch {
        run()
    }

    suspend fun run() {
        while (true) {
            currentPhase = currentPhase.step(this) ?: break
        }
    }
}
