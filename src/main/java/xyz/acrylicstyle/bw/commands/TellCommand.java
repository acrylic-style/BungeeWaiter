package xyz.acrylicstyle.bw.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import util.ICollectionList;

public class TellCommand extends Command implements TabExecutor {
    public TellCommand() {
        super("gtell", null, "gw", "gmsg");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "Please specify a player!"));
            return;
        }
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(args[0]);
        if (player == null) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "That player is not online."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "Please specify a message!"));
            return;
        }
        ICollectionList<String> messages = ICollectionList.asList(args);
        messages.shift();
        String message = messages.join(" ");
        sender.sendMessage(new TextComponent(ChatColor.GRAY + "[" + sender.getName() + " -> " + player.getName() + "] " + ChatColor.WHITE + message));
        player.sendMessage(new TextComponent(ChatColor.GRAY + "[" + sender.getName() + " -> " + player.getName() + "] " + ChatColor.WHITE + message));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return VersionsCommand.playerTabCompleter(args);
    }
}
