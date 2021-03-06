package network.cow.indigo.client.spigot.command

import io.grpc.Status
import network.cow.cowmands.Arguments
import network.cow.cowmands.Cowmand
import network.cow.indigo.client.spigot.IndigoPlugin
import network.cow.indigo.client.spigot.createRoleIdentifierOf
import network.cow.indigo.client.spigot.handleGrpc
import network.cow.mooapis.indigo.v1.RemoveRolePermissionsRequest
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

/**
 * @author Tobias Büser
 */
class RolesPermissionRemoveCommand(val plugin: IndigoPlugin) : Cowmand() {

    override val label = "remove"
    override val usage = "<target> <permission>"

    override fun execute(sender: CommandSender, args: Arguments) {
        if (args.size <= 1) {
            sender.sendMessage("§c/roles permission remove <target> <permission>")
            return
        }
        val name = args[0]
        val permission = args[1]

        val role = plugin.cache.getRole(name)
        if (role == null) {
            sender.sendMessage("§cRole does not exist.")
            return
        }
        if (!role.permissionsList.contains(permission)) {
            sender.sendMessage("§cRole does not have this permission.")
            return
        }

        val status = handleGrpc {
            plugin.stub.removeRolePermissions(
                RemoveRolePermissionsRequest.newBuilder()
                    .setRoleId(createRoleIdentifierOf(name))
                    .addPermissions(permission)
                    .build()
            )
        }
        if (!status.isOk()) {
            status.handle(Status.Code.NOT_FOUND) {
                sender.sendMessage("§4Service could not find role.")
            }.handleCommandDefault(sender)
            return
        }

        sender.sendMessage("§aPermission removed.")
    }

    override fun tabComplete(sender: CommandSender, args: Arguments): List<String> {
        return if (args.size == 1) {
            plugin.cache.getRoles().map { it.name } + Bukkit.getOnlinePlayers().map { "@${it.name}" }
        } else emptyList()
    }

}
