//package tororo1066.mahjongmc
//
//import org.bukkit.Location
//import tororo1066.displaymonitorapi.actions.IActionContext
//import tororo1066.displaymonitorapi.actions.IPublicActionContext
//import tororo1066.mahjongmc.game.MahjongInstance.Companion.appendSuffix
//import tororo1066.mahjongmc.dmonitor.workspace.MahjongWorkspace
//import tororo1066.mahjongmc.enums.PlayerSettings
//import tororo1066.mahjongmc.tile.HonorTiles
//import tororo1066.mahjongmc.tile.Tile
//import tororo1066.mahjongmc.tile.TileType
//import tororo1066.tororopluginapi.utils.toPlayer
//import java.util.concurrent.CompletableFuture
//
//class MahjongTableInstance(val location: Location) {
//    val players = mutableListOf<PlayerInstance>()
//    val generalTiles = mutableListOf<Tile>()
//    val deadWall = DeadWall()
//    var playerSettings = PlayerSettings.PLAYERS_4
//
//    val publicActionContext = MahjongMc.createPublicActionContext().apply {
//        workspace = MahjongWorkspace
//    }
//
//    fun parameterizeContext(): IPublicActionContext {
//        return publicActionContext.apply {
//            parameters.let {
//                it["players"] = players.associate { player ->
//                    player.uuid.toString() to mapOf(
//                        "uuid" to player.uuid.toString(),
//                        "name" to player.name,
//                        "position" to player.position.name,
//                        "rotation" to player.position.yaw,
//                        "score" to player.score,
//                        "holdTimes" to player.holdTimes,
//                        "backupTimes" to player.backupTimes
//                    )
//                }
//            }
//        }
//    }
//
//    fun setUpTiles() {
//        generalTiles.clear()
//        for (type in TileType.entries.filter { it != TileType.HONORS }) {
//            for (i in 1..9) {
//                for (j in 0 until 4) {
//                    if (playerSettings == PlayerSettings.PLAYERS_3 && type == TileType.BAMBOO && i in 2..8) {
//                        continue
//                    }
//                    val tile = Tile(
//                        type = type,
//                        number = i,
//                        isRed = i == 5 && j == 0
//                     )
//
//                    generalTiles.add(tile)
//                }
//            }
//        }
//
//        repeat(4) {
//            for (honorType in 0 until 4) {
//                val tile = Tile(
//                    type = TileType.HONORS,
//                    number = 0,
//                    honor = HonorTiles.entries[honorType]
//                )
//                generalTiles.add(tile)
//            }
//        }
//
//        generalTiles.shuffle()
//
//        players.forEach { instance ->
//            instance.tiles.clear()
//            repeat(13) {
//                val tile = generalTiles.removeFirst()
//                instance.tiles.add(tile)
//            }
//        }
//
//        deadWall.setUpDeadWall(generalTiles, playerSettings)
//    }
//
//    fun spawnTable() {
//        players.forEach {
//            it.mahjongTableCostume.let { costume ->
//                MahjongMc.actionRunner.run(
//                    MahjongMc.emptyAdvancedConfiguration(),
//                    costume.getOnSpawn(),
//                    MahjongMc.createActionContext(parameterizeContext()).apply {
//                        this.location = this@MahjongTableInstance.location
//                        this.target = it.uuid.toPlayer()
//                        this.prepareParameters.let { params ->
//                            params["display"] = MahjongInstance.TABLE_DISPLAY.appendSuffix(it.uuid.toString())
//                        }
//                    },
//                    null,
//                    true,
//                    false
//                )
//            }
//        }
//    }
//
//    fun spawnPrepareTiles(items: IntRange = 0 until 13, onInteract: (tile: Tile, context: IActionContext) -> Unit) {
//        players.forEach { instance ->
//            items.forEach { index ->
//                val tile = instance.tiles[index]
//                tile.spawnEntity(
//                    instance = this,
//                    index = index,
//                    player = instance
//                ) { context ->
//                    onInteract(tile, context)
//                }
//            }
//        }
//    }
//
//    fun sortTiles(player: PlayerInstance, receiver: PlayerInstance, before: List<Tile>? = null): CompletableFuture<Void> {
//        val before = before ?: player.tiles.toList()
//        val current = if (player == receiver) {
//            player.tiles
//        } else {
//            receiver.otherPlayerDisplayTiles[player.uuid] ?: mutableListOf()
//        }
//
//        if (player == receiver) {
//            current.sortWith(compareBy({ it.type.ordinal }, { it.number }, { it.honor.ordinal }))
//        }
//
//        val completableFutures = mutableListOf<CompletableFuture<Void>>()
//        current.forEachIndexed { index, tile ->
//            if (before.indexOf(tile) != index) {
//                //位置が変わっていたら移動アクションを実行
//                val task = MahjongMc.actionRunner.run(
//                    MahjongMc.emptyAdvancedConfiguration(),
//                    receiver.tileCostume.getOnMove(),
//                    MahjongMc.createActionContext(parameterizeContext()).apply {
//                        this.location = receiver.mahjongTableCostume.getTileLocation(
//                            this@MahjongTableInstance.location,
//                            index,
//                            player.position,
//                            lastTile = false
//                        )
//                        this.target = receiver.getPlayer()
//                        this.prepareParameters.let {
//                            it["tile"] = tile.parameters()
//                            it["display"] = MahjongInstance.TILE_DISPLAY.appendSuffix(tile.uuid.toString())
//                            it["hidden"] = receiver != player
//                        }
//                    },
//                    null,
//                    true,
//                    false
//                )
//                completableFutures.add(task)
//            }
//        }
//
//        return CompletableFuture.allOf(*completableFutures.toTypedArray())
//    }
//}