package tororo1066.mahjongmc.mahjong

import tororo1066.mahjongmc.Call
import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.tile.Tile
import tororo1066.mahjongmc.tile.TileType
import tororo1066.mahjongmc.mahjong.yaku.bonus.*
import tororo1066.mahjongmc.mahjong.yaku.doubleYakuman.*
import tororo1066.mahjongmc.mahjong.yaku.one.*
import tororo1066.mahjongmc.mahjong.yaku.other.*
import tororo1066.mahjongmc.mahjong.yaku.six.*
import tororo1066.mahjongmc.mahjong.yaku.three.*
import tororo1066.mahjongmc.mahjong.yaku.two.*
import tororo1066.mahjongmc.mahjong.yaku.yakuman.*
import kotlin.math.pow

object WinningManager {

    val winnings: List<AbstractWinning> = listOf(
        // 1翻
        AllSimples,
        DeadWallDraw,
        HonorTilesDragons,
        HonorTilesOwnWind,
        HonorTilesRoundWind,
        LastTileClaim,
        LastTileDraw,
        Pinfu,
        Richi,
        RobbingAQuad,
        Tsumo,
        TwinSequences,

        // 2翻
        DoubleRichi,
        SevenPairs,
        LittleThreeDragons,
        ThreeConsecutiveSequences,
        FourTriplets,
        ThreeConcealedTriplets,
        ThreeFours,
        ThreeSimilarSequences,
        ThreeSimilarTriplets,
        TerminalOrHonorInEachMeld,
        AllTerminalsAndHonors,

        // 3翻
        OneSuitWithHonors,
        TerminalInEachMeld,
        TwoTwinSequences,

        // 6翻
        OneSuitOnly,

        // 役満
        AllGreen,
        AllHonors,
        AllTerminals,
        BigThreeDragons,
        LittleFourWinds,
        HeavenlyHand,
        EarthlyHand,
        FourConcealedTriplets,
        FourFours,
        NineGates,
        ThirteenOrphans,

        // ダブル役満
        BigFourWinds,
        FourConcealedTripletsPairWait,
        ThirteenWaitThirteenOrphans,
        TrueNineGates,

        // ボーナス
        Bonus,
        HiddenBonus,
        RedBonus,
        OneShot,

        // 役なし
        DiscardTerminalsAndHonorsThroughout
    )

    fun tilesToCounts(tiles: List<Tile>): IntArray {
        val arr = IntArray(34) { 0 }
        tiles.forEach {
            arr[it.getIndex()] += 1
        }
        return arr
    }

    /**
     * 手牌と鳴きを元に和了形かどうかを判定する
     *
     * calls(副露)はすでに完成面子として固定し、手牌側で不足する面子数+雀頭を作れるかを判定する。
     * 七対子/国士無双は副露がある時点で成立しない前提のため、callsが空のときのみ判定する。
     *
     * @param tiles 手牌（副露に使った牌は含めない想定）
     * @param calls 鳴き
     * @return 和了形であればtrue、そうでなければfalse
     */
    fun isWinningHand(tiles: List<Tile>, calls: List<Call>): Boolean {
        val counts = tilesToCounts(tiles)

        val requiredMelds = 4 - calls.size
        if (requiredMelds < 0) return false

        // 鳴きがない場合、七対子/国士無双の和了も確認する
        if (calls.isEmpty()) {
            if (checkSevenPairs(counts) || checkThirteenOrphans(counts)) return true
        }

        return checkNormalWithRequiredMelds(counts, requiredMelds)
    }

    /**
     * call(暗槓のみ想定)を考慮して立直の捨て牌候補を返す。
     *
     * NOTE: 現状の仕様では「暗槓のみ」を有効なcallsとして扱う。暗槓以外が混ざる場合は空集合を返す。
     */
    fun richiDiscards(tiles: List<Tile>, calls: List<Call>): Set<Tile> {
        if (calls.any { it.type != Call.Type.CONCEALED_KAN }) return emptySet()

        val result = mutableSetOf<Tile>()

        for (groupTiles in tiles.groupBy { it.getIndex() }.values) {
            val tempTiles = tiles.toMutableList()
            tempTiles.remove(groupTiles.first())

            if (isTenpai(tempTiles, calls)) {
                result.addAll(groupTiles)
            }
        }

        return result
    }

    fun isTenpai(tiles: List<Tile>, calls: List<Call>): Boolean {
        val counts = tilesToCounts(tiles)
        return isTenpai(counts, calls)
    }

    private fun isTenpai(counts: IntArray, calls: List<Call>): Boolean {
        val requiredMelds = 4 - calls.size
        if (requiredMelds < 0) return false

        // 引数countsを破壊しない
        for (i in 0 until 34) {
            if (counts[i] >= 4) continue
            val next = counts.copyOf()
            next[i] += 1

            // 鳴きがない場合、七対子/国士無双の聴牌も確認する
            if (calls.isEmpty()) {
                if (checkSevenPairs(next) || checkThirteenOrphans(next)) return true
            }

            if (checkNormalWithRequiredMelds(next, requiredMelds)) return true
        }
        return false
    }

    fun getTenpaiTiles(tiles: List<Tile>, calls: List<Call>): Set<Tile> {
        val result = mutableSetOf<Tile>()
        val counts = tilesToCounts(tiles)

        val requiredMelds = 4 - calls.size
        if (requiredMelds < 0) return emptySet()

        for (i in 0 until 34) {
            if (counts[i] >= 4) continue
            val next = counts.copyOf()
            next[i] += 1

            // 鳴きがない場合、七対子/国士無双の聴牌も確認する
            if (calls.isEmpty()) {
                if (checkSevenPairs(next) || checkThirteenOrphans(next)) {
                    result.add(Tile.fromIndex(i))
                    continue
                }
            }

            if (checkNormalWithRequiredMelds(next, requiredMelds)) {
                result.add(Tile.fromIndex(i))
            }
        }

        return result
    }


    fun findBestWinning(
        instance: MahjongInstance,
        player: PlayerInstance,
        tiles: List<Tile>,
        winningTile: Tile,
        isTsumo: Boolean
    ): Winning? {
        var bestWinning: Winning? = null

        val winningStructures = findWinningStructures(
            tiles,
            player.calls,
            winningTile,
            isTsumo
        )

        for (structure in winningStructures) {
            val winnings = findWinnings(instance, player, structure, isTsumo)
            if (winnings.isEmpty()) continue
            if (!validateWinnings(winnings)) continue

            val (score, winningType) = getWinningScore(instance, player, structure, winnings, isTsumo)
            if (bestWinning == null || score > bestWinning.baseScore) {
                bestWinning = Winning(
                    winnings,
                    structure,
                    score,
                    winningType
                )
            }
        }

        return bestWinning
    }

    fun findWinnings(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        isTsumo: Boolean
    ): List<AbstractWinning> {
        val result = mutableListOf<AbstractWinning>()
        for (yaku in winnings) {
            if (yaku.check(instance, player, winningStructure, isTsumo)) {
                result.add(yaku)
            }
        }

        for (winning in result.toList()) {
            if (winning.conflictsWith.any { it in result }) {
                result.remove(winning)
            }
        }

        if (winnings.any { it is AbstractYakumanWinning }) {
            // 役満がある場合、役満以外はすべて無効
            return result.filterIsInstance<AbstractYakumanWinning>()
        }

        return result
    }

    fun validateWinnings(
        winnings: List<AbstractWinning>
    ): Boolean {
        return winnings.any { it.hasYaku }
    }

    fun getFu(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        winnings: List<AbstractWinning>,
        isTsumo: Boolean
    ): Int {
        if (winnings.any { it is Pinfu }) {
            return if (isTsumo) 20 else 30
        }

        if (winnings.any { it is SevenPairs }) {
            return 25
        }

        var fu = if (!isTsumo && player.calls.isEmpty()) 30 else 20
        if (isTsumo) {
            fu += 2 // 自摸和の符
        }

        for (meld in winningStructure.winningTiles.melds) {
            if (meld is Meld.Sequence) {
                continue // 順子は符がつかない
            }
            val isTermsOrHonor = meld.tiles[0].isTermsOrHonors()
            val called = (!isTsumo && meld.tiles.contains(winningStructure.winningTile))
                    || meld.isCalled

            when(meld) {
                is Meld.Triplet -> {
                    fu += when {
                        isTermsOrHonor && !called -> 8
                        isTermsOrHonor && called -> 4
                        !isTermsOrHonor && !called -> 4
                        else -> 2
                    }
                }
                is Meld.Quadruplet -> {
                    fu += when {
                        isTermsOrHonor && !called -> 32
                        isTermsOrHonor && called -> 16
                        !isTermsOrHonor && !called -> 16
                        else -> 8
                    }
                }

                else -> {}
            }
        }

        // 雀頭の符
        val head = winningStructure.winningTiles.heads.firstOrNull()
        if (head != null) {
            if (head.type == TileType.HONORS) {
                if (head.honor.isDragon()) {
                    fu += 2
                } else {
                    val position = head.honor.getPosition()
                    if (position == instance.roundWind || position == player.position) {
                        fu += 2
                    }
                }
            }
        }

        // 待ちの符
        val waitType = winningStructure.getWaitType()
        when (waitType) {
            WaitType.TANKI, WaitType.PENCHAN, WaitType.KANCHAN -> fu += 2
            else -> {}
        }

        // 切り上げ
        return if (fu % 10 == 0) fu else ((fu / 10) + 1) * 10
    }

    data class WinningScore(
        val baseScore: Int,
        val winningType: WinningType
    )

    fun getWinningScore(
        instance: MahjongInstance,
        player: PlayerInstance,
        winningStructure: WinningStructure,
        winnings: List<AbstractWinning>,
        isTsumo: Boolean
    ): WinningScore {
        val yakumanWinnings = winnings.filterIsInstance<AbstractYakumanWinning>()
        if (yakumanWinnings.isNotEmpty()) {
//            return yakumanWinnings.sumOf { it.yakumanValue } * 8000
            val yakumanValue = yakumanWinnings.sumOf { it.yakumanValue }
            val winningType = when (yakumanValue) {
                1 -> WinningType.YAKUMAN
                2 -> WinningType.DOUBLE_YAKUMAN
                3 -> WinningType.TRIPLE_YAKUMAN
                else -> WinningType.YAKUMAN
            }
            return WinningScore(
                baseScore = yakumanValue * 8000,
                winningType = winningType
            )
        }

        val fu = getFu(instance, player, winningStructure, winnings, isTsumo)
        val han = winnings.sumOf { winning ->
            winning.getHan(instance, player, winningStructure, isTsumo)
        }

        var score = (2.0.pow(han + 2) * fu).toInt()
        val winningType: WinningType

        if (score >= 2000 || han >= 5) {
            when {
                han >= 13 -> {
                    winningType = WinningType.COUNTED_YAKUMAN
                    score = 8000
                }
                han >= 11 -> {
                    winningType = WinningType.SANBAIMAN
                    score = 6000
                }
                han >= 8 -> {
                    winningType = WinningType.BAIMAN
                    score = 4000
                }
                han >= 6 -> {
                    winningType = WinningType.HANEMAN
                    score = 3000
                }
                else -> {
                    winningType = WinningType.MANGAN
                    score = 2000
                }
            }
        } else {
            score = ((score + 99) / 100) * 100 // 100点単位に切り上げ
            winningType = WinningType.NORMAL
        }

        return WinningScore(
            baseScore = score,
            winningType = winningType
        )
    }

    fun paymentNagashiManganWinning(
        instance: MahjongInstance,
        player: PlayerInstance
    ): Int {
        val isDealer = player.position == instance.dealerPosition

        var winningScore = 0

        instance.players.forEach { p ->
            if (p == player) return@forEach
            val isPayerDealer = p.position == instance.dealerPosition
            val payment = if (isDealer || isPayerDealer) 4000 else 2000

            p.score -= payment
            player.score += payment

            winningScore += payment
        }

        return winningScore
    }

    fun paymentTsumoWinning(
        instance: MahjongInstance,
        player: PlayerInstance,
        winning: Winning,
        richiSticks: Int,
        continueCount: Int
    ): Int {
        val (winnings, _, baseScore) = winning
        if (winnings.any { it is AbstractYakumanWinning }) {
            return paymentYakumanTsumoWinning(instance, player, winning, richiSticks, continueCount)
        }

        val isDealer = player.position == instance.dealerPosition
        val continuePayment = continueCount * 100

        var winningScore = 0

        instance.players.forEach { p ->
            if (p == player) return@forEach
            val isPayerDealer = p.position == instance.dealerPosition
            val winningPayment = if (isDealer || isPayerDealer) baseScore * 2 else baseScore
            val totalPayment = winningPayment + continuePayment
            p.score -= totalPayment
            player.score += totalPayment
            winningScore += winningPayment
        }

        player.score += richiSticks * 1000

        return winningScore
    }

    fun paymentYakumanTsumoWinning(
        instance: MahjongInstance,
        player: PlayerInstance,
        winning: Winning,
        richiSticks: Int,
        continueCount: Int
    ): Int {
        val (winnings, _, _) = winning
        val yakumanWinnings = winnings.filterIsInstance<AbstractYakumanWinning>()
        if (yakumanWinnings.isEmpty()) return 0

        val isDealer = player.position == instance.dealerPosition

        val responsiblePaymentPlayers = mutableSetOf<PlayerInstance>()
        var winningScore = 0

        yakumanWinnings.forEach { winning ->
            val baseScore = winning.yakumanValue * 8000
            if (winning is AbstractYakumanResponsiblePaymentWinning) {
                val target = winning.checkTarget(player)
                val targetPlayer = instance.players.find { it.position == target } ?: return@forEach
                if (target != null) {
                    val totalPayment = instance.players.filter { it.position != target }.sumOf { p ->
                        if (isDealer || p.position == instance.dealerPosition) baseScore * 2 else baseScore
                    }

                    responsiblePaymentPlayers.add(targetPlayer)

                    targetPlayer.score -= totalPayment
                    player.score += totalPayment

                    winningScore += totalPayment
                    return@forEach
                }
            }

            instance.players.forEach { p ->
                if (p == player) return@forEach
                val isPayerDealer = p.position == instance.dealerPosition
                val payment = if (isDealer || isPayerDealer) baseScore * 2 else baseScore
                p.score -= payment
                player.score += payment
                winningScore += payment
            }
        }

        if (responsiblePaymentPlayers.isNotEmpty()) {
            // 包の対象となったプレイヤーから積み棒の分も徴収する(全員徴収するかは要検討)
            val continuePayment = continueCount * 100 * (instance.players.size - 1)
            responsiblePaymentPlayers.forEach { p ->
                p.score -= continuePayment
                player.score += continuePayment
            }
        } else {
            // 包がない場合は全員から積み棒の分も徴収する
            val coutinuePayment = continueCount * 100
            instance.players.forEach { p ->
                if (p == player) return@forEach
                p.score -= coutinuePayment
                player.score += coutinuePayment
            }
        }

        player.score += richiSticks * 1000

        return winningScore
    }

    fun paymentRonWinning(
        instance: MahjongInstance,
        player: PlayerInstance,
        payer: PlayerInstance,
        winning: Winning,
        richiSticks: Int,
        continueCount: Int
    ): Int {
        val (winnings, _, baseScore) = winning
        if (winnings.any { it is AbstractYakumanWinning }) {
            return paymentYakumanRonWinning(instance, player, payer, winning, richiSticks, continueCount)
        }

        val isDealer = player.position == instance.dealerPosition
        val isPayerDealer = payer.position == instance.dealerPosition

        val winningPayment = if (isDealer || isPayerDealer) baseScore * 6 else baseScore * 4
        val continuePayment = continueCount * 300

        val totalPayment = winningPayment + continuePayment

        payer.score -= totalPayment
        player.score += totalPayment

        player.score += richiSticks * 1000

        return winningPayment
    }

    fun paymentYakumanRonWinning(
        instance: MahjongInstance,
        player: PlayerInstance,
        payer: PlayerInstance,
        winning: Winning,
        richiSticks: Int,
        continueCount: Int
    ): Int {
        val (winnings, _, _) = winning
        val yakumanWinnings = winnings.filterIsInstance<AbstractYakumanWinning>()
        if (yakumanWinnings.isEmpty()) return 0

        val isDealer = player.position == instance.dealerPosition
        val isPayerDealer = payer.position == instance.dealerPosition

        val responsiblePaymentPlayers = mutableSetOf<PlayerInstance>()
        var winningScore = 0

        yakumanWinnings.forEach { winning ->
            val payment = winning.yakumanValue * 8000 * if (isDealer || isPayerDealer) 6 else 4
            if (winning is AbstractYakumanResponsiblePaymentWinning) {
                val target = winning.checkTarget(player)
                val targetPlayer = instance.players.find { it.position == target } ?: return@forEach
                if (target != null) {
                    val halfPayment = payment / 2
                    responsiblePaymentPlayers.add(targetPlayer)

                    payer.score -= halfPayment
                    targetPlayer.score -= halfPayment
                    player.score += payment

                    winningScore += payment

                    return@forEach
                }
            }

            payer.score -= payment
            player.score += payment
            winningScore += payment
        }

        val continuePayment = continueCount * 300

        if (responsiblePaymentPlayers.isNotEmpty()) {
            responsiblePaymentPlayers.forEach { p ->
                p.score -= continuePayment
                player.score += continuePayment
            }
        } else {
            payer.score -= continuePayment
            player.score += continuePayment
        }

        player.score += richiSticks * 1000

        return winningScore
    }

    private sealed class MeldIndex {
        data class Triplet(val index: Int): MeldIndex()
        data class Sequence(val index: Int): MeldIndex()
        data class Single(val index: Int): MeldIndex()
        data class Pair(val index: Int): MeldIndex()
    }

    private data class State(
        val counts: IntArray,
        val head: Int?,
        val melds: List<MeldIndex>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as State

            if (!counts.contentEquals(other.counts)) return false
            if (head != other.head) return false
            if (melds != other.melds) return false

            return true
        }

        override fun hashCode(): Int {
            var result = counts.contentHashCode()
            result = 31 * result + (head?.hashCode() ?: 0)
            result = 31 * result + melds.hashCode()
            return result
        }
    }

    fun findWinningStructures(
        tiles: List<Tile>,
        calls: List<Call>,
        winningTile: Tile,
        isTsumo: Boolean
    ): List<WinningStructure> {

        fun IntArray.copyAndDecrement(index: Int, amount: Int = 1): IntArray =
            this.copyOf().also { it[index] -= amount }

        val meldsFromCalls = calls.map { call -> Meld.byCall(call) }
        val meldCountFromCalls = meldsFromCalls.size

        val results = mutableListOf<State>()

        fun search(state: State) {
            if (state.melds.size + meldCountFromCalls > 4) return

            if (state.counts.all { it == 0 }) {
                if (state.head != null && state.melds.size + meldCountFromCalls == 4) {
                    results.add(state)
                }
                return
            }

            val first = state.counts.indexOfFirst { it > 0 }
            val newCounts = state.counts.copyOf()

            // 雀頭候補
            if (newCounts[first] >= 2 && state.head == null) {
                search(state.copy(
                    counts = newCounts.copyAndDecrement(first, 2),
                    head = first
                ))
            }

            // 刻子候補
            if (newCounts[first] >= 3) {
                search(
                    state.copy(
                        counts = newCounts.copyAndDecrement(first, 3),
                        melds = state.melds + MeldIndex.Triplet(first)
                    )
                )
            }

            // 順子候補
            if (first <= 24 && first % 9 <= 6 && newCounts[first + 1] > 0 && newCounts[first + 2] > 0) {
                search(
                    state.copy(
                        counts = newCounts
                            .copyAndDecrement(first)
                            .copyAndDecrement(first + 1)
                            .copyAndDecrement(first + 2),
                        melds = state.melds + MeldIndex.Sequence(first)
                    )
                )
            }
        }

        val initialState = State(
            counts = tilesToCounts(tiles),
            head = null,
            melds = emptyList()
        )

        search(initialState)

        if (calls.isEmpty()) {
            findSevenPairs(tiles)?.let { results.add(it) }
            findThirteenOrphans(tiles)?.let { results.add(it) }
        }

        return results.map { state ->
            restoreWinningStructure(state, tiles, meldsFromCalls, winningTile, isTsumo)
        }
    }

    private fun findThirteenOrphans(
        tiles: List<Tile>
    ): State? {
        val counts = tilesToCounts(tiles)
        val requiredIndices = setOf(
            0, 8, 9, 17, 18, 26, // 1と9の数牌
            27, 28, 29, 30, 31, 32, 33 // 字牌
        )

        var pairIndex: Int? = null
        for (i in requiredIndices) {
            if (counts[i] == 0) return null
            if (counts[i] >= 2) {
                if (pairIndex != null) return null
                pairIndex = i
            }
        }
        if (pairIndex == null) return null

        return State(
            counts = counts,
            head = pairIndex,
            melds = requiredIndices.filter { it != pairIndex }.map { MeldIndex.Single(it) }
        )
    }

    private fun findSevenPairs(
        tiles: List<Tile>
    ): State? {
        val counts = tilesToCounts(tiles)
        val pairs = mutableListOf<Int>()
        for (i in 0 until 34) {
            if (counts[i] == 2) {
                pairs.add(i)
            } else if (counts[i] != 0 && counts[i] != 2) {
                return null
            }
        }
        if (pairs.size != 7) return null

        return State(
            counts = counts,
            head = null,
            melds = pairs.map { MeldIndex.Pair(it) }
        )
    }

    private fun restoreWinningStructure(
        state: State,
        tiles: List<Tile>,
        meldsFromCalls: List<Meld>,
        winningTile: Tile,
        isTsumo: Boolean
    ): WinningStructure {
        val pools = tiles.groupBy { it.getIndex() }
            .mapValues { entry -> entry.value.toMutableList() }

        val melds = state.melds.map { meldIndex ->
            when (meldIndex) {
                is MeldIndex.Triplet -> {
                    val tiles = List(3) {
                        pools[meldIndex.index]!!.removeFirst()
                    }
                    Meld.Triplet(
                        tiles,
                        isCalled = !isTsumo && tiles.contains(winningTile)
                    )
                }
                is MeldIndex.Sequence -> {
                    val tiles = List(3) {
                        pools[meldIndex.index + it]!!.removeFirst()
                    }
                    Meld.Sequence(
                        tiles,
                        isCalled = !isTsumo && tiles.contains(winningTile)
                    )
                }
                is MeldIndex.Single -> {
                    Meld.Single(
                        pools[meldIndex.index]!!.removeFirst()
                    )
                }
                is MeldIndex.Pair -> {
                    Meld.Pair(
                        listOf(
                            pools[meldIndex.index]!!.removeFirst(),
                            pools[meldIndex.index]!!.removeFirst()
                        )
                    )
                }
            }
        }

        val headTiles = if (state.head != null) {
            listOf(
                pools[state.head]!!.removeFirst(),
                pools[state.head]!!.removeFirst()
            )
        } else {
            emptyList()
        }

        val winningTiles = WinningTiles(
            heads = headTiles,
            melds = melds + meldsFromCalls
        )
        return WinningStructure(winningTiles, winningTile)
    }

    fun checkThirteenOrphans(tiles: List<Tile>): Boolean {
        val counts = tilesToCounts(tiles)
        return checkThirteenOrphans(counts)
    }

    private fun checkSevenPairs(counts: IntArray): Boolean {
        var pairCount = 0
        for (i in 0 until 34) {
            if (counts[i] == 2) {
                pairCount++
            } else if (counts[i] != 0 && counts[i] != 2) {
                return false
            }
        }
        return pairCount == 7
    }

    private fun checkThirteenOrphans(counts: IntArray): Boolean {
        val requiredIndices = setOf(
            0, 8, 9, 17, 18, 26, // 1と9の数牌
            27, 28, 29, 30, 31, 32, 33 // 字牌
        )
        var hasPair = false
        for (i in requiredIndices) {
            if (counts[i] == 0) return false
            if (counts[i] >= 2) {
                if (hasPair) return false
                hasPair = true
            }
        }
        return hasPair
    }

    /**
     * calls(副露)で面子が固定されている場合向けの通常形判定。
     * requiredMelds 個の面子 + 雀頭1つを手牌(counts)から作れるかを判定する。
     */
    private fun checkNormalWithRequiredMelds(counts: IntArray, requiredMelds: Int): Boolean {
        if (requiredMelds !in 0..4) return false

        // 面子が0個必要な場合でも、雀頭は必ず必要（通常形）
        for (i in 0 until 34) {
            if (counts[i] >= 2) {
                val temp = counts.copyOf()
                temp[i] -= 2
                if (isMentsuCompleteWithLimit(temp, requiredMelds)) return true
            }
        }
        return false
    }

    /**
     * counts から requiredMelds 個の面子（刻子/順子）を作り切れて、かつ残りが0になるかをDFSで判定する。
     * 既存の isMentsuComplete は貪欲なため、鳴き込み判定の正確性を優先して分岐探索を行う。
     */
    private fun isMentsuCompleteWithLimit(counts: IntArray, requiredMelds: Int): Boolean {
        if (requiredMelds < 0) return false
        if (requiredMelds == 0) return counts.all { it == 0 }

        val first = counts.indexOfFirst { it > 0 }
        if (first == -1) return false // requiredMelds > 0 なのに牌が残っていない

        // 刻子
        if (counts[first] >= 3) {
            val next = counts.copyOf()
            next[first] -= 3
            if (isMentsuCompleteWithLimit(next, requiredMelds - 1)) return true
        }

        // 順子
        if (first <= 24 && first % 9 <= 6 && counts[first + 1] > 0 && counts[first + 2] > 0) {
            val next = counts.copyOf()
            next[first]--
            next[first + 1]--
            next[first + 2]--
            if (isMentsuCompleteWithLimit(next, requiredMelds - 1)) return true
        }

        return false
    }
}