package xyz.acrylicstyle.bw.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import util.ICollectionList;

public class GKickCommand extends Command implements TabExecutor {
    public GKickCommand() {
        super("gkick", "bungeewaiter.gkick");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "/gkick <player>"));
            return;
        }
        ProxiedPlayer p = ProxyServer.getInstance().getPlayer(args[0]);
        if (p == null) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "Could not find player."));
            return;
        }
        ICollectionList<String> list = ICollectionList.asList(args);
        list.shift();
        String reason = list.size() == 0 ? "You have been kicked by an operator." : list.join(" ");
        p.disconnect(new TextComponent(ChatColor.translateAlternateColorCodes('&', reason)));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return VersionsCommand.playerTabCompleter(args);
    }
}
