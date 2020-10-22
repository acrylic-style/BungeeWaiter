package xyz.acrylicstyle.bw.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.jetbrains.annotations.NotNull;
import util.CollectionList;
import util.ICollectionList;
import xyz.acrylicstyle.bw.BungeeWaiter;
import xyz.acrylicstyle.mcutil.lang.MCVersion;

import java.util.Collections;

import static xyz.acrylicstyle.bw.BungeeWaiter.bool;

public class VersionsCommand extends Command implements TabExecutor {
    public VersionsCommand() {
        super("versions", "bungeewaiter.versions");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            PlayersCommand.sendPlayersByVersion(sender);
        } else {
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(args[0]);
            if (player == null) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "That player is not online."));
                return;
            }
            if (args.length >= 2) {
                String subarg = args[1];
                if (subarg.equalsIgnoreCase("mods")) {
                    if (!player.isForgeUser()) {
                        sender.sendMessage(new TextComponent(ChatColor.GREEN + "This player is not using Forge!"));
                        return;
                    }
                    sender.sendMessage(new TextComponent(ChatColor.YELLOW + player.getName() + ChatColor.GREEN + "'s mods:"));
                    player.getModList().forEach((mod, ver) -> sender.sendMessage(new TextComponent(ChatColor.YELLOW + " - " + ChatColor.GOLD + mod + ChatColor.DARK_GRAY + "   v" + ChatColor.GRAY + ver)));
                } else {
                    sender.sendMessage(new TextComponent(ChatColor.RED + "Acceptable arguments: " + ChatColor.YELLOW + SUB_ARGUMENTS.join(ChatColor.GRAY + ", " + ChatColor.YELLOW)));
                }
                return;
            }
            MCVersion version = BungeeWaiter.getReleaseVersionIfPossible(player.getPendingConnection().getVersion());
            String versions = ICollectionList.asList(MCVersion.getByProtocolVersion(player.getPendingConnection().getVersion())).map(MCVersion::getName).join(ChatColor.YELLOW + ", " + ChatColor.AQUA);
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + player.getName() + ChatColor.AQUA + ":"));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + " - Version: " + ChatColor.AQUA + version.getName()));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + " - All possible versions: " + ChatColor.AQUA + versions));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + " - Modern(1.13+): " + bool(version.isModern())));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + " - Pre-release: " + bool(version.isPrerelease())));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + " - Snapshot: " + bool(version.isSnapshot())));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + " - Release Candidate: " + bool(version.isReleaseCandidate())));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + " - Protocol Version: " + ChatColor.RED + player.getPendingConnection().getVersion()));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + " - Virtual Host: " + ChatColor.AQUA + player.getPendingConnection().getVirtualHost().getHostName()));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + " - Label: " + ChatColor.LIGHT_PURPLE + BungeeWaiter.getLabel(player)));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + " - Connection Type: " + BungeeWaiter.getConnectionTypeColored(player)));
            String path = BungeeWaiter.getConnectionPath(player);
            if (path != null) sender.sendMessage(new TextComponent(ChatColor.YELLOW + " - Connection Path: " + ChatColor.GREEN + path));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + " - Using Forge: " + bool(player.isForgeUser())));
            /*
            if (player.isForgeUser()) {
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + " - " + ChatColor.RED + player.getModList().size() + ChatColor.YELLOW + " mods installed"));
                String mods = ChatColor.GOLD + ICollection.asCollection(player.getModList()).valuesList().join(ChatColor.GRAY + ", " + ChatColor.GOLD);
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + " - Mods: " + mods));
            }
            */
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) return SUB_ARGUMENTS.filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()));
        return playerTabCompleter(args);
    }

    private static final CollectionList<String> SUB_ARGUMENTS = new CollectionList<>("mods");

    @NotNull
    public static Iterable<String> playerTabCompleter(String[] args) {
        if (args.length == 0) return new CollectionList<>(ProxyServer.getInstance().getPlayers()).map(ProxiedPlayer::getName);
        if (args.length == 1) return new CollectionList<>(ProxyServer.getInstance().getPlayers()).map(ProxiedPlayer::getName).filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()));
        return Collections.emptyList();
    }
}
