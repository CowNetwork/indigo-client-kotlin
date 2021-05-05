package network.cow.indigo.client.spigot.command

import network.cow.indigo.client.spigot.IndigoPlugin
import network.cow.indigo.client.spigot.cowmand.Arguments
import network.cow.indigo.client.spigot.cowmand.Cowmand
import network.cow.indigo.client.spigot.handleGrpc
import network.cow.mooapis.indigo.v1.RemoveUserRolesRequest
import network.cow.mooapis.indigo.v1.RoleIdentifier
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

/**
 * @author Tobias Büser
 */
class RolesRemoveCommand(private val plugin: IndigoPlugin) : Cowmand() {

    override val label = "remove"
    override val usage = "<player> <role>"

    override fun execute(sender: CommandSender, args: Arguments) {
        if (args.size <= 1) {
            sender.sendMessage("§c/roles remove <player> <role>")
            return
        }
        val playerName = args[0]
        val roleName = args[0]

        val player = Bukkit.getPlayer(playerName)
        if (player == null) {
            sender.sendMessage("§cThis player is not online!")
            return
        }

        val role = plugin.roleCache.getRole(roleName)
        if (role == null) {
            sender.sendMessage("§cRole does not exist.")
            return
        }

        val indigoUser = plugin.userCache.getUser(player.uniqueId)
        if (indigoUser != null && !indigoUser.hasRole(roleName)) {
            sender.sendMessage("§cThis player does not have this role.")
            return
        }

        val status = handleGrpc {
            plugin.stub.removeUserRoles(
                RemoveUserRolesRequest.newBuilder()
                    .setUserAccountId(player.uniqueId.toString())
                    .addRoleIds(RoleIdentifier.newBuilder().setUuid(role.id).build())
                    .build()
            )
        }
        if (!status.isOk()) {
            status.handleCommandDefault(sender)
            return
        }
        sender.sendMessage("§aRole removed.")
    }

    override fun tabComplete(sender: CommandSender, args: Arguments): List<String> {
        return if (args.isEmpty()) {
            Bukkit.getOnlinePlayers().map { it.name }
        } else {
            plugin.roleCache.getRoles().map { it.name }
        }
    }
}
