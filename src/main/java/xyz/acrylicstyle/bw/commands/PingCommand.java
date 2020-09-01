package xyz.acrylicstyle.bw.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class PingCommand extends Command {
    public PingCommand() {
        super("gping");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "This command cannot be invoked from console."));
            return;
        }
        if (args.length == 0) {
            ProxiedPlayer player = (ProxiedPlayer) sender;
            sender.sendMessage(new TextComponent(ChatColor.GREEN + "Ping: " + getPingMessage(player.getPing())));
            return;
        }
        String p = args[0];
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(p);
        if (player == null) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "Player could not be found."));
            return;
        }
        sender.sendMessage(new TextComponent(ChatColor.GREEN + player.getName() + "'s Ping: " + getPingMessage(player.getPing())));
    }

    public static String getPingMessage(int ping) {
        String message;
        if (ping <= 5) message = "" + ChatColor.LIGHT_PURPLE + ping;
        else if (ping <= 50) message = "" + ChatColor.GREEN + ping;
        else if (ping <= 200) message = "" + ChatColor.YELLOW + ping;
        else if (ping <= 350) message = "" + ChatColor.GOLD + ping;
        else if (ping <= 500) message = "" + ChatColor.RED + ping;
        else message = "" + ChatColor.DARK_RED + ping;
        return message + "ms";
    }
}
