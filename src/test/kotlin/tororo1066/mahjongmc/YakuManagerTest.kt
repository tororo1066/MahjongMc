package tororo1066.mahjongmc

import be.seeseemelk.mockbukkit.MockBukkit
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tororo1066.mahjongmc.enums.Position
import tororo1066.mahjongmc.mahjong.WinningManager
import tororo1066.mahjongmc.tile.HonorTiles
import tororo1066.mahjongmc.tile.Tile
import tororo1066.mahjongmc.tile.TileType

class YakuManagerTest {

    @BeforeEach
    fun setup() {
        MockBukkit.mock()
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun testWinningTiles() {
        val tiles = listOf(
            Tile(TileType.CHARACTERS, 2),
            Tile(TileType.CHARACTERS, 3),
            Tile(TileType.CHARACTERS, 4),
            Tile(TileType.DOTS, 5),
            Tile(TileType.DOTS, 6),
            Tile(TileType.DOTS, 7),
            Tile(TileType.BAMBOO, 3),
            Tile(TileType.BAMBOO, 3),
            Tile(TileType.BAMBOO, 3),
            Tile(TileType.CHARACTERS, 5),
            Tile(TileType.CHARACTERS, 6),
            Tile(TileType.CHARACTERS, 7),
            Tile(TileType.HONORS, honor = HonorTiles.EAST),
            Tile(TileType.HONORS, honor = HonorTiles.EAST)
        )
        val result = WinningManager.findWinningTiles(tiles, emptyList())
        println(result)
        assert(result.isNotEmpty())
    }

    @Test
    fun testWinningTiles2() {
        val tiles = listOf(
            Tile(TileType.CHARACTERS, 1),
            Tile(TileType.CHARACTERS, 1),
            Tile(TileType.CHARACTERS, 1),
            Tile(TileType.CHARACTERS, 2),
            Tile(TileType.CHARACTERS, 2),
            Tile(TileType.CHARACTERS, 2),
            Tile(TileType.CHARACTERS, 3),
            Tile(TileType.CHARACTERS, 3),
            Tile(TileType.CHARACTERS, 3),
            Tile(TileType.DOTS, 7),
            Tile(TileType.DOTS, 8),
            Tile(TileType.DOTS, 9),
            Tile(TileType.HONORS, honor = HonorTiles.RED),
            Tile(TileType.HONORS, honor = HonorTiles.RED)
        )

        val result = WinningManager.findWinningTiles(tiles, emptyList())
        println(result)
        assert(result.size == 2)
    }

    @Test
    fun testWinningTiles3() {
        val tiles = listOf(
            Tile(TileType.CHARACTERS, 2),
            Tile(TileType.CHARACTERS, 3),
            Tile(TileType.CHARACTERS, 4),
            Tile(TileType.DOTS, 5),
            Tile(TileType.DOTS, 6),
            Tile(TileType.DOTS, 7),
            Tile(TileType.BAMBOO, 3),
            Tile(TileType.BAMBOO, 3),
            Tile(TileType.BAMBOO, 3),
            Tile(TileType.CHARACTERS, 5),
            Tile(TileType.CHARACTERS, 6),
            Tile(TileType.CHARACTERS, 7),
            Tile(TileType.HONORS, honor = HonorTiles.EAST),
            Tile(TileType.HONORS, honor = HonorTiles.SOUTH)
        )
        val result = WinningManager.findWinningTiles(tiles, emptyList())
        println(result)
        assert(result.isEmpty())
    }

    @Test
    fun testWinningTiles4() {
        val tiles = listOf(
            Tile(TileType.CHARACTERS, 2),
            Tile(TileType.CHARACTERS, 3),
            Tile(TileType.CHARACTERS, 4),
            Tile(TileType.CHARACTERS, 5),
            Tile(TileType.CHARACTERS, 6),
            Tile(TileType.CHARACTERS, 7),
            Tile(TileType.HONORS, honor = HonorTiles.EAST),
            Tile(TileType.HONORS, honor = HonorTiles.EAST)
        )

        val calls = listOf(
            Call(
                type = Call.Type.CHI,
                tiles = listOf(
                    Tile(TileType.CHARACTERS, 3),
                    Tile(TileType.CHARACTERS, 4),
                    Tile(TileType.CHARACTERS, 5)
                ),
                target = Position.EAST,
                calledTile = Tile(TileType.CHARACTERS, 5)
            ),
            Call(
                type = Call.Type.CHI,
                tiles = listOf(
                    Tile(TileType.CHARACTERS, 4),
                    Tile(TileType.CHARACTERS, 5),
                    Tile(TileType.CHARACTERS, 6)
                ),
                target = Position.EAST,
                calledTile = Tile(TileType.CHARACTERS, 6)
            )
        )

        val result = WinningManager.findWinningTiles(tiles, calls)
        println(result)
        assert(result.size == 1)
    }
}