package xyz.acrylicstyle.bw.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import util.CollectionList;
import xyz.acrylicstyle.bw.BungeeWaiter;
import xyz.acrylicstyle.sql.TableData;
import xyz.acrylicstyle.sql.options.FindOptions;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.UUID;

public class AltLookupCommand extends Command implements TabExecutor {
    public AltLookupCommand() {
        super("altlookup", "bungeewaiter.altlookup", "al");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "/altlookup <PlayerName or IP>"));
            return;
        }
        InetAddress address;
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(args[0]);
        if (player == null) {
            try {
                address = InetAddress.getByName(args[0]);
            } catch (UnknownHostException e) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "Could not resolve hostname by " + ChatColor.GOLD + args[0]));
                return;
            }
        } else {
            if (!(player.getSocketAddress() instanceof InetSocketAddress)) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "This player isn't connecting via IP Address."));
                return;
            }
            address = ((InetSocketAddress) player.getSocketAddress()).getAddress();
        }
        if (address instanceof Inet4Address) {
            // ipv4
            BungeeWaiter.db.lastIpV4.findAll(new FindOptions.Builder().addWhere("ip", address.getHostAddress()).build()).then(list -> handler(list, sender, args[0])).queue();
        } else {
            // ipv6
            BungeeWaiter.db.lastIpV6.findAll(new FindOptions.Builder().addWhere("ip", address.getHostAddress()).build()).then(list -> handler(list, sender, args[0])).queue();
        }
    }

    private static Void handler(CollectionList<TableData> list, CommandSender sender, String target) {
        sender.sendMessage(new TextComponent(ChatColor.LIGHT_PURPLE + "Lookup result of " + ChatColor.YELLOW + target + ChatColor.LIGHT_PURPLE + ":"));
        list.forEach(td -> {
            String name = td.getString("name");
            UUID uuid = UUID.fromString(td.getString("uuid"));
            String playerName = name == null ? ChatColor.GRAY + "Unknown" + ChatColor.DARK_GRAY + "<" + uuid.toString() + ">" : ChatColor.GOLD + name;
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + " - " + playerName));
        });
        return null;
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return VersionsCommand.playerTabCompleter(args);
    }
}
