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

import java.net.Inet6Address;
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
        new Thread(() -> {
            if (args.length == 0) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "/altlookup <PlayerName, UUID, or IP>"));
                return;
            }
            String address = null;
            boolean ipv4 = true;
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(args[0]);
            try {
                TableData td = BungeeWaiter.db.lastIpV4.findOne(new FindOptions.Builder().addWhere("uuid", UUID.fromString(args[0]).toString()).build()).complete();
                if (td == null) {
                    sender.sendMessage(new TextComponent(ChatColor.RED + "Could not lookup IP from UUID by " + ChatColor.GOLD + args[0]));
                    return;
                }
                try {
                    InetAddress addr = InetAddress.getByName(td.getString("ip"));
                    if (addr instanceof Inet6Address) ipv4 = false;
                    address = addr.getHostAddress();
                } catch (UnknownHostException ex) {
                    throw new RuntimeException(ex);
                }
            } catch (IllegalArgumentException ignored) {}
            if (address == null) {
                if (player == null) {
                    try {
                        InetAddress addr = InetAddress.getByName(args[0]);
                        if (addr instanceof Inet6Address) ipv4 = false;
                        address = addr.getHostAddress();
                    } catch (UnknownHostException e) {
                        TableData td = BungeeWaiter.db.lastIpV4.findOne(new FindOptions.Builder().addWhere("name", args[0]).build()).complete();
                        if (td == null) {
                            sender.sendMessage(new TextComponent(ChatColor.RED + "Could not resolve hostname / player name (case sensitive) by " + ChatColor.GOLD + args[0]));
                            return;
                        }
                        try {
                            InetAddress addr = InetAddress.getByName(td.getString("ip"));
                            if (addr instanceof Inet6Address) ipv4 = false;
                            address = addr.getHostAddress();
                        } catch (UnknownHostException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                } else {
                    if (!(player.getSocketAddress() instanceof InetSocketAddress)) {
                        sender.sendMessage(new TextComponent(ChatColor.RED + "This player isn't connecting via IP Address."));
                        return;
                    }
                    InetAddress addr = ((InetSocketAddress) player.getSocketAddress()).getAddress();
                    if (addr instanceof Inet6Address) ipv4 = false;
                    address = addr.getHostAddress();
                }
            }
            if (ipv4) {
                // ipv4
                BungeeWaiter.db.lastIpV4.findAll(new FindOptions.Builder().addWhere("ip", address).build()).then(list -> handler(list, sender, args[0])).queue();
            } else {
                // ipv6
                BungeeWaiter.db.lastIpV6.findAll(new FindOptions.Builder().addWhere("ip", address).build()).then(list -> handler(list, sender, args[0])).queue();
            }
        }).start();
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
