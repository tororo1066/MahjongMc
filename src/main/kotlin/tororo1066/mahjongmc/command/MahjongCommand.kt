package tororo1066.mahjongmc.command

import org.bukkit.entity.Player
import tororo1066.commandapi.argumentType.EntityArg
import tororo1066.commandapi.argumentType.StringArg
import tororo1066.mahjongmc.costume.CostumeFactory
import tororo1066.mahjongmc.costume.EffectCostume
import tororo1066.mahjongmc.costume.MahjongTableCostume
import tororo1066.mahjongmc.costume.RichiStickCostume
import tororo1066.mahjongmc.costume.TileCostume
import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.tile.Tile
import tororo1066.tororopluginapi.annotation.SCommandV2Body
import tororo1066.tororopluginapi.sCommand.v2.SCommandV2
import tororo1066.tororopluginapi.utils.setPitchL
import tororo1066.tororopluginapi.utils.setYawL

class MahjongCommand: SCommandV2("mahjong") {

    var currentInstance: MahjongInstance? = null

    @SCommandV2Body
    val test = command {
        literal("test") {
            argument("player", EntityArg(singleTarget = true, playersOnly = true)) {
                setPlayerFunctionExecutor { sender, _, args ->
                    val target = args.getEntities("player").first() as Player

                    test(sender, target)
                }
                argument("tiles", StringArg.greedyPhrase()) {
                    setPlayerFunctionExecutor { sender, _, args ->

                        val target = args.getEntities("player").first() as Player
                        val tilesString = args.getArgument("tiles", String::class.java)

                        test(sender, target, tilesString)
                    }
                }
            }
        }

        literal("stop") {
            setPlayerFunctionExecutor { sender, _, _ ->
                currentInstance?.stop()
                currentInstance = null
            }
        }
    }

    fun test(sender: Player, target: Player, tilesString: String? = null) {
        try {
            CostumeFactory.reloadDefaults()
            val tileCostume = TileCostume.createEmpty()
            val tableCostume = MahjongTableCostume.createEmpty()
            val effectCostume = EffectCostume.createEmpty()
            val richiStickCostume = RichiStickCostume.createEmpty()
            val location = sender.location.setYawL(0f).setPitchL(0f)
            val instance = MahjongInstance(location)

            val tiles = mutableListOf<Tile>()
            if (tilesString != null) {
                val tileIndices = tilesString.split(",").mapNotNull { it.toIntOrNull() }
                for (tileIndex in tileIndices) {
                    val tile = Tile.fromIndex(tileIndex)
                    tiles.add(tile)
                }
            }

            instance.players.add(PlayerInstance().apply {
                uuid = sender.uniqueId
                name = sender.name
                costumes.addAll(listOf(tableCostume, tileCostume, effectCostume, richiStickCostume))
                this.debugPrepareTiles.addAll(tiles)
            })
            instance.players.add(PlayerInstance().apply {
                uuid = target.uniqueId
                name = target.name
                costumes.addAll(listOf(tableCostume, tileCostume, effectCostume, richiStickCostume))
            })

            currentInstance = instance

            instance.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}