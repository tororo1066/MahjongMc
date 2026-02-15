package tororo1066.mahjongmc.game

import tororo1066.displaymonitorapi.actions.IActionContext
import tororo1066.mahjongmc.Call
import tororo1066.mahjongmc.Call.Companion.excludeConcealedKan
import tororo1066.mahjongmc.MahjongMc
import tororo1066.mahjongmc.costume.AbstractCostume
import tororo1066.mahjongmc.enums.PlayerSettings
import tororo1066.mahjongmc.enums.Position
import tororo1066.mahjongmc.mahjong.WinningManager
import tororo1066.mahjongmc.tile.Tile
import tororo1066.mahjongmc.tile.TileType
import tororo1066.tororopluginapi.utils.toPlayer
import java.util.UUID
import kotlin.collections.iterator
import kotlin.math.min

class PlayerInstance {
    lateinit var uuid: UUID
    lateinit var name: String
    var score: Int = 25000
    var holdTime: Int = 10
    var backupTime: Int = 30

    var isRichi: Boolean = false
    var isDoubleRichi: Boolean = false
    var isOneShot: Boolean = false // 一発

    var ignoredRonOnRichi: Boolean = false // リーチ後のフリテン
    var ignoredRonOnSilent: Boolean = false // 同巡内フリテン

    var position: Position = Position.EAST
    //手牌
    val tiles = mutableListOf<Tile>()
    val otherPlayerDisplayTiles = mutableMapOf<UUID, MutableList<Tile>>()
    //捨て牌
    val discard = mutableListOf<Tile>()
    val otherPlayerDisplayDiscard = mutableMapOf<UUID, MutableList<Tile>>()
    //ポン、チー、カンの牌
    val calls = mutableListOf<Call>()

    val allHandTiles: List<Tile>
        get() {
            val handTiles = tiles.toMutableList()
            calls.forEach { call ->
                handTiles.addAll(call.tiles)
            }
            return handTiles
        }

    val costumes = mutableListOf<AbstractCostume>()

    val publicActionContext by lazy { MahjongMc.createPublicActionContext() }

    inline fun <reified T: AbstractCostume> getCostume(): T {
        return costumes.filterIsInstance<T>().first()
    }

    fun getPlayer() = uuid.toPlayer()

    fun initCostumes(instance: MahjongInstance) {
        costumes.forEach { costume ->
            costume.init(instance)
        }
    }

    fun initRound() {
        isRichi = false
        isDoubleRichi = false
        isOneShot = false
        ignoredRonOnRichi = false
        ignoredRonOnSilent = false
        tiles.clear()
        otherPlayerDisplayTiles.clear()
        discard.clear()
        otherPlayerDisplayDiscard.clear()
        calls.clear()
        holdTime = 10
    }

    fun setNextTimes(holdTime: Int, backupTime: Int) {
        val used = this.holdTime + this.backupTime - holdTime - backupTime
        this.backupTime = if (used <= 5) {
            min(30, backupTime + 10)
        } else {
            min(30, backupTime + 5)
        }
        this.holdTime = 10
    }

    fun canPon(tile: Tile, instance: MahjongInstance): Boolean {
        if (isRichi) return false
        // 海底牌はポン不可
        if (instance.generalTiles.isEmpty()) return false
        return tiles.count {
            it.isSameTile(tile)
        } >= 2
    }

    //ポンの選択肢 赤牌の場合は赤牌を選択肢に含める
    fun ponChoices(tile: Tile): List<List<Tile>> {
        val ponChoices = mutableListOf<List<Tile>>()

        val ponTiles = tiles.filter {
            it.isSameTile(tile)
        }

        if (ponTiles.none { it.isRed } || ponTiles.size == 2) {
            ponChoices.add(ponTiles.take(2).plus(tile))
        } else {
            for (i in 0..min(ponTiles.count { it.isRed }, 2)) {
                val pon = ponTiles.filter { it.isRed }.take(i) + ponTiles.filter { !it.isRed }.take(2 - i)
                if (pon.size == 2) {
                    ponChoices.add(pon.plus(tile))
                }
            }
        }

        return ponChoices
    }

    fun canChi(tile: Tile, instance: MahjongInstance): Boolean {
        // 3人麻雀ではチー不可
        if (instance.settings.playerSettings == PlayerSettings.PLAYERS_3) return false
        if (isRichi) return false
        // 海底牌はチー不可
        if (instance.generalTiles.isEmpty()) return false
        // 字牌はチー不可
        if (tile.type == TileType.HONORS) return false

        val numbers = tiles.filter { it.type == tile.type }.map { it.number }.toSet()
        if (!((tile.number - 2 in numbers && tile.number - 1 in numbers) ||
                (tile.number - 1 in numbers && tile.number + 1 in numbers) ||
                (tile.number + 1 in numbers && tile.number + 2 in numbers))) {
            return false
        }

        if (instance.settings.allowKuigae) return true

        val choices = chiChoices(tile)

        // 喰い替えになる牌しか残らなくなる場合は不可
        return choices.any {
            val tempCall = Call(Call.Type.CHI, it, position, tile)
            val tempTiles = tiles.toMutableList()
            tempTiles.removeAll(it)
            val kuigaeTiles = getKuigaeTiles(tempCall, tempTiles)
            tempTiles.size - kuigaeTiles.size >= 1
        }
    }

    //チーの選択肢 赤牌の場合は赤牌を選択肢に含める
    fun chiChoices(tile: Tile): List<List<Tile>> {
        val chiChoices = mutableListOf<List<Tile>>()
        val candidates = tiles.filter { it.type == tile.type }
            .groupBy { it.number }
            .mapValues { (_, tiles) ->
                val (reds, normals) = tiles.partition { it.isRed }
                listOfNotNull(
                    reds.firstOrNull(),
                    normals.firstOrNull()
                )
            }

        val possibleSequences = listOf(
            listOf(tile.number - 2, tile.number - 1),
            listOf(tile.number - 1, tile.number + 1),
            listOf(tile.number + 1, tile.number + 2)
        )

        for (sequence in possibleSequences) {
            val firstTiles = candidates[sequence[0]] ?: continue
            val secondTiles = candidates[sequence[1]] ?: continue

            for (first in firstTiles) {
                for (second in secondTiles) {
                    chiChoices.add(listOf(first, second, tile))
                }
            }
        }

        return chiChoices
    }

    fun canKan(tile: Tile, instance: MahjongInstance): Boolean {
        if (isRichi) return false
        // 海底牌はカン不可
        if (instance.generalTiles.isEmpty()) return false
        return tiles.count {
            it.isSameTile(tile)
        } >= 3
    }

    fun kanChoices(tile: Tile): List<List<Tile>> {
        val kanChoices = mutableListOf<List<Tile>>()

        val kanTiles = tiles.filter {
            it.isSameTile(tile)
        }

        if (kanTiles.none { it.isRed } || kanTiles.size == 3) {
            kanChoices.add(kanTiles.take(3).plus(tile))
        } else {
            for (i in 0..min(kanTiles.count { it.isRed }, 3)) {
                val kan = kanTiles.filter { it.isRed }.take(i) + kanTiles.filter { !it.isRed }.take(3 - i)
                if (kan.size == 3) {
                    kanChoices.add(kan.plus(tile))
                }
            }
        }

        return kanChoices
    }

    fun canConcealedKan(instance: MahjongInstance): Boolean {
        // 海底牌は暗槓不可
        if (instance.generalTiles.isEmpty()) return false

        val group = tiles.groupBy { it.type to it.number to it.honor }
            .filter { (_, groupedTiles) -> groupedTiles.size >= 4 }

        if (group.isEmpty()) return false

        for ((_, groupedTiles) in group) {
            val kanTiles = groupedTiles.take(4)
            if (!allowedConcealedKan(kanTiles)) {
                return false
            }
        }
        return true
    }

    fun concealedKanChoices(): List<List<Tile>> {
        val kanChoices = mutableListOf<List<Tile>>()

        val group = tiles.groupBy { it.type to it.number to it.honor }
            .filter { (_, groupedTiles) -> groupedTiles.size >= 4 }

        for ((_, groupedTiles) in group) {
            val kanTiles = groupedTiles.take(4)
            if (allowedConcealedKan(kanTiles)) {
                kanChoices.add(kanTiles)
            }
        }
        return kanChoices
    }

    private fun allowedConcealedKan(
        kanTiles: List<Tile>
    ): Boolean {
        if (!isRichi) return true

        val lastTile = tiles.lastOrNull() ?: return false
        // 送り槓は不可
        if (!kanTiles.contains(lastTile)) {
            return false
        }

        val tempTiles = tiles.toMutableList()
        tempTiles.remove(lastTile)

        // 暗槓後に聴牌の種類が変わる場合は不可
        val currentTenpaiTiles = WinningManager.getTenpaiTiles(tempTiles, calls)

        val tempCalls = calls.toMutableList()

        tempTiles.removeAll(kanTiles)
        tempCalls.add(Call(Call.Type.CONCEALED_KAN, kanTiles, position, kanTiles[0]))
        val newTenpaiTiles = WinningManager.getTenpaiTiles(tempTiles, tempCalls)
        // 聴牌の中身(種類)が変わらなければOK
        return currentTenpaiTiles.size == newTenpaiTiles.size &&
                currentTenpaiTiles.all { tile -> newTenpaiTiles.any { it.isSameTile(tile) } }
    }

    fun canLateKan(instance: MahjongInstance): Boolean {
        // 海底牌は加槓不可
        if (instance.generalTiles.isEmpty()) return false

        for (call in calls) {
            if (call.type == Call.Type.PON) {
                val ponTile = call.calledTile
                val matchingTiles = tiles.filter { it.isSameTile(ponTile) }
                if (matchingTiles.isNotEmpty()) {
                    return true
                }
            }
        }
        return false
    }

    fun lateKanChoices(): List<List<Tile>> {
        val kanChoices = mutableListOf<List<Tile>>()

        for (call in calls) {
            if (call.type == Call.Type.PON) {
                val ponTile = call.calledTile
                val matchingTile = tiles.find { it.isSameTile(ponTile) }
                if (matchingTile != null) {
                    kanChoices.add(call.tiles + matchingTile)
                }
            }
        }

        return kanChoices
    }


    fun canRichi(instance: MahjongInstance): Boolean {
        if (isRichi) return false
        if (calls.excludeConcealedKan().isNotEmpty()) return false
        if (score < 1000) return false
        val requiredGeneralTiles = instance.settings.playerSettings.seats
        if (instance.generalTiles.size < requiredGeneralTiles) return false

        return WinningManager.richiDiscards(tiles).isNotEmpty()
    }

    fun canTsumo(instance: MahjongInstance): Boolean {
        return isWinningHand(instance, tiles = tiles, tile = tiles.last(), isTsumo = true)
    }

    fun canRon(tile: Tile, instance: MahjongInstance): Boolean {

        if (ignoredRonOnRichi || ignoredRonOnSilent) {
            return false
        }

        if (!isWinningHand(instance, tiles = tiles + tile, tile = tile, isTsumo = false)) {
            return false
        }

        val tenpaiTiles = WinningManager.getTenpaiTiles(tiles, calls)
        // 自分で捨てた牌の中に聴牌牌がある場合はロン不可
        return !discard.any { discardedTile -> tenpaiTiles.any { it.isSameTile(discardedTile) } }
    }

    private fun isWinningHand(instance: MahjongInstance, tiles: List<Tile>, tile: Tile, isTsumo: Boolean): Boolean {
        if (!WinningManager.isWinningHand(tiles, calls)) {
            return false
        }
        WinningManager.findBestWinning(
            instance = instance,
            player = this,
            tiles = tiles,
            winningTile = tile,
            isTsumo = isTsumo
        ) ?: return false
        return true
    }

    // 喰い替え判定になる牌を取得
    fun getKuigaeTiles(
        lastCall: Call,
        tiles: List<Tile> = this.tiles
    ): List<Tile> {
        when (lastCall.type) {
            Call.Type.PON -> {
                // ポンされた牌と同じ牌は喰い替え判定になる
                return tiles.filter { tile ->
                    tile.isSameTile(lastCall.calledTile)
                }
            }
            Call.Type.CHI -> {
                val calledTiles = lastCall.tiles
                val minNumber = calledTiles.minOf { it.number }
                val maxNumber = calledTiles.maxOf { it.number }

                val kuigaeTiles = mutableListOf<Tile>()

                for (tile in tiles) {
                    if (tile.type != calledTiles[0].type) continue

                    if (tile.isSameTile(lastCall.calledTile)) {
                        // チーされた牌は喰い替え判定になる
                        kuigaeTiles.add(tile)
                        continue
                    }
                    if (tile.number == minNumber - 1 || tile.number == maxNumber + 1) {
                        kuigaeTiles.add(tile)
                    }
                }

                return kuigaeTiles
            }
            else -> return emptyList()
        }
    }

    fun createContext(instance: MahjongInstance): IActionContext {
        return MahjongMc.createActionContext(publicActionContext).apply {
            target = getPlayer()
            prepareParameters.let {
                it["centerLocation"] = mapOf(
                    "world" to instance.centerLocation.world!!.name,
                    "x" to instance.centerLocation.x,
                    "y" to instance.centerLocation.y,
                    "z" to instance.centerLocation.z,
                    "yaw" to instance.centerLocation.yaw,
                    "pitch" to instance.centerLocation.pitch
                )
                it["players"] = instance.players.map { player -> player.uuid.toString() }
                it["playersData"] = instance.players.associate { player ->
                    player.uuid.toString() to mapOf(
                        "uuid" to player.uuid.toString(),
                        "name" to player.name,
                        "position" to player.position.name,
                        "rotation" to player.position.getMinecraftYaw(instance.dealerPosition, instance.settings.playerSettings),
                        "score" to player.score,
                        "holdTimes" to player.holdTime,
                        "backupTimes" to player.backupTime
                    )
                }
            }
        }
    }
}