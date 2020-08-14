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
import util.CollectionSet;
import util.ICollectionList;
import util.MultiCollection;
import xyz.acrylicstyle.bw.BungeeWaiter;
import xyz.acrylicstyle.mcutil.lang.MCVersion;

import java.util.Collections;
import java.util.function.Function;

import static xyz.acrylicstyle.bw.BungeeWaiter.bool;

public class VersionsCommand extends Command implements TabExecutor {
    public VersionsCommand() {
        super("versions", "bungeewaiter.versions");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            CollectionList<String> messages = new CollectionList<>();
            CollectionSet<MCVersion> versions = new CollectionSet<>();
            MultiCollection<MCVersion, String> players = new MultiCollection<>();
            ProxyServer.getInstance().getPlayers().forEach(player -> {
                MCVersion v = BungeeWaiter.getReleaseVersionIfPossible(player.getPendingConnection().getVersion());
                versions.add(v);
                players.add(v, player.getName());
            });
            CollectionList<MCVersion> versions2 = ICollectionList.asList(versions);
            versions2.sort((a, b) -> b.getProtocolVersion() - a.getProtocolVersion());
            versions2.forEach(version -> messages.add(ChatColor.LIGHT_PURPLE + "[" + (version.isModern() ? ChatColor.GREEN : ChatColor.YELLOW) + version.getName() + ChatColor.LIGHT_PURPLE + "] " + ChatColor.YELLOW + "(" + ChatColor.RED + players.get(version).size() + ChatColor.YELLOW + ")" + ChatColor.WHITE + ": " + ChatColor.GREEN + players.get(version).join(ChatColor.YELLOW + ", " + ChatColor.GREEN) + "\n"));
            sender.sendMessage(messages.map((Function<String, TextComponent>) TextComponent::new).toArray(new TextComponent[0]));
        } else {
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(args[0]);
            if (player == null) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "That player is not online."));
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
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return playerTabCompleter(args);
    }

    @NotNull
    public static Iterable<String> playerTabCompleter(String[] args) {
        if (args.length == 0) return new CollectionList<>(ProxyServer.getInstance().getPlayers()).map(ProxiedPlayer::getName);
        if (args.length == 1) return new CollectionList<>(ProxyServer.getInstance().getPlayers()).map(ProxiedPlayer::getName).filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()));
        return Collections.emptyList();
    }
}
