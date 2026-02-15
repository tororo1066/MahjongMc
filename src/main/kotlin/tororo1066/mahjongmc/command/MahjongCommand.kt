package tororo1066.mahjongmc.command

import org.bukkit.configuration.file.YamlConfiguration
import org.yaml.snakeyaml.Yaml
import tororo1066.commandapi.argumentType.EntityArg
import tororo1066.commandapi.argumentType.StringArg
import tororo1066.mahjongmc.MahjongMc
import tororo1066.mahjongmc.costume.EffectCostume
import tororo1066.mahjongmc.game.MahjongInstance
import tororo1066.mahjongmc.game.PlayerInstance
import tororo1066.mahjongmc.costume.MahjongTableCostume
import tororo1066.mahjongmc.costume.TileCostume
import tororo1066.mahjongmc.tile.Tile
import tororo1066.tororopluginapi.SJavaPlugin
import tororo1066.tororopluginapi.annotation.SCommandV2Body
import tororo1066.tororopluginapi.sCommand.v2.SCommandV2
import tororo1066.tororopluginapi.utils.setPitchL
import tororo1066.tororopluginapi.utils.setYawL
import java.io.File

class MahjongCommand: SCommandV2("mahjong") {

    @SCommandV2Body
    val test = command {
        literal("test") {
            setPlayerFunctionExecutor { sender, _, _ ->
                try {
                    val tileCostume = TileCostume.create()
                    val tableCostume = MahjongTableCostume.create()
                    val location = sender.location.setYawL(0f).setPitchL(0f)
                    val instance = MahjongInstance(location)

                    instance.players.add(PlayerInstance().apply {
                        uuid = sender.uniqueId
                        name = sender.name
                        costumes.addAll(listOf(tableCostume, tileCostume))
                    })
                    instance.start()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        literal("test2") {
            argument("player", EntityArg(singleTarget = true, playersOnly = true)) {
                setPlayerFunctionExecutor { sender, _, args ->

                    val target = args.getEntities("player").first()

                    try {
                        val tileCostume = TileCostume.create()
                        val tableCostume = MahjongTableCostume.create()
                        val location = sender.location.setYawL(0f).setPitchL(0f)
                        val instance = MahjongInstance(location)

                        instance.players.add(PlayerInstance().apply {
                            uuid = sender.uniqueId
                            name = sender.name
                            costumes.addAll(listOf(tableCostume, tileCostume))
                        })
                        instance.players.add(PlayerInstance().apply {
                            uuid = target.uniqueId
                            name = target.name
                            costumes.addAll(listOf(tableCostume, tileCostume))
                        })
                        instance.start()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        literal("test3") {
            argument("player", EntityArg(singleTarget = true, playersOnly = true)) {
                argument("tiles", StringArg.greedyPhrase()) {
                    setPlayerFunctionExecutor { sender, _, args ->

                        val target = args.getEntities("player").first()
                        val tilesString = args.getArgument("tiles", String::class.java)

                        try {
                            val tileCostume = TileCostume.create()
                            val tableCostume = MahjongTableCostume.create()
                            val effectCostume = EffectCostume.create()
                            val location = sender.location.setYawL(0f).setPitchL(0f)
                            val instance = MahjongInstance(location, debug = true)

                            val tiles = mutableListOf<Tile>()
                            for (tileStr in tilesString.split(",")) {
                                val tile = Tile.fromIndex(tileStr.toInt())
                                tiles.add(tile)
                            }

                            instance.players.add(PlayerInstance().apply {
                                uuid = sender.uniqueId
                                name = sender.name
                                costumes.addAll(listOf(tableCostume, tileCostume, effectCostume))
                                this.tiles.addAll(tiles)
                            })
                            instance.players.add(PlayerInstance().apply {
                                uuid = target.uniqueId
                                name = target.name
                                costumes.addAll(listOf(tableCostume, tileCostume, effectCostume))
                            })

                            instance.start()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        literal("test4") {
            setPlayerFunctionExecutor { sender, _, _ ->
                val yamlString = """
                    test: null
                """.trimIndent()
                val yaml = Yaml()
                val data = yaml.load<MutableMap<String, Any?>>(yamlString)
                sender.sendMessage(data.toString())
            }
        }
    }
}