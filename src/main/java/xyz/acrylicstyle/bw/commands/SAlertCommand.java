package xyz.acrylicstyle.bw.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import util.CollectionList;
import util.ICollectionList;

import java.util.Collections;

public class SAlertCommand extends Command implements TabExecutor {
    public SAlertCommand() {
        super("salert", "bungeewaiter.salert");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "Please specify a server!"));
            return;
        }
        ServerInfo server = ProxyServer.getInstance().getServerInfo(args[0]);
        if (server == null) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "Could not find server by " + args[0]));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "Please specify a message!"));
            return;
        }
        ICollectionList<String> messages = ICollectionList.asList(args);
        messages.shift();
        String message = messages.join(" ");
        sender.sendMessage(new TextComponent(ChatColor.GRAY + "[" + sender.getName() + " -> {" + server.getName() + "}] " + message));
        server.getPlayers().forEach(player -> player.sendMessage(new TextComponent(ChatColor.LIGHT_PURPLE + sender.getName() + ChatColor.GREEN + ": " + ChatColor.translateAlternateColorCodes('&', message))));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 0) return new CollectionList<>(ProxyServer.getInstance().getServers().values()).map(ServerInfo::getName);
        if (args.length == 1) return new CollectionList<>(ProxyServer.getInstance().getServers().values()).map(ServerInfo::getName).filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()));
        return Collections.emptyList();
    }
}
