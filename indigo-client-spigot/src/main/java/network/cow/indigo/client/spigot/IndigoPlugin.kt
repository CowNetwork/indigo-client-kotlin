package network.cow.indigo.client.spigot

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import network.cow.cloudevents.CloudEventsService
import network.cow.cowmands.Cowmands
import network.cow.grape.Grape
import network.cow.indigo.client.spigot.api.IndigoService
import network.cow.indigo.client.spigot.command.RolesCommand
import network.cow.indigo.client.spigot.listener.PlayerListener
import network.cow.indigo.client.spigot.listener.RoleUpdateCloudEventListener
import network.cow.indigo.client.spigot.listener.UserPermissionUpdateCloudEventListener
import network.cow.mooapis.indigo.v1.IndigoServiceGrpc
import network.cow.mooapis.indigo.v1.RoleUpdateEvent
import network.cow.mooapis.indigo.v1.UserPermissionUpdateEvent
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.TimeUnit

/**
 * @author Tobias Büser
 */
class IndigoPlugin : JavaPlugin() {

    private lateinit var channel: ManagedChannel
    lateinit var stub: IndigoServiceGrpc.IndigoServiceBlockingStub

    lateinit var cache: IndigoCache
    lateinit var indigoConfig: IndigoConfig

    override fun onEnable() {
        this.indigoConfig = IndigoConfig(
            this.config.getString("defaultRole"),
            this.config.getBoolean("assignDefaultRole"),
            IndigoConfig.Connection(
                this.config.getString("service.host", "localhost")!!,
                this.config.getInt("service.port", 6969)
            )
        )

        this.channel = ManagedChannelBuilder.forAddress(indigoConfig.connection.host, indigoConfig.connection.port)
            .usePlaintext()
            .build()

        this.stub = IndigoServiceGrpc.newBlockingStub(channel)

        Cowmands.register(this, RolesCommand(this))
        Bukkit.getPluginManager().registerEvents(PlayerListener(this), this)

        // load all roles and permission
        this.cache = IndigoCache(stub, this)
        this.cache.loadRolesFromService()
        if (this.cache.getRole(this.indigoConfig.defaultRole) == null) {
            logger.warning("Default role ${this.indigoConfig.defaultRole} does not exist.")
        }

        // cloud events
        val service = Grape.getInstance()[CloudEventsService::class.java].getNow(null)
        if (service == null) {
            logger.warning("Could not find CloudEventsService, disabling ..")
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }
        service.consumer.listen(
            "cow.indigo.v1.RoleUpdateEvent",
            RoleUpdateEvent::class.java,
            RoleUpdateCloudEventListener(this)
        )
        service.consumer.listen(
            "cow.indigo.v1.UserPermissionUpdateEvent",
            UserPermissionUpdateEvent::class.java,
            UserPermissionUpdateCloudEventListener(this)
        )

        Grape.getInstance().register(IndigoService::class.java, SimpleIndigoService(this))
    }

    override fun onDisable() {
        try {
            this.channel.shutdown().awaitTermination(2, TimeUnit.SECONDS)
        } catch (ex: Exception) {
            logger.info("Error during shutdown, ignoring ..")
        }
    }

}
