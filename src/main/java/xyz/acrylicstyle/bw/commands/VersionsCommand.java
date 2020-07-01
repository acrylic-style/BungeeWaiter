package xyz.acrylicstyle.bw.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import util.CollectionList;
import util.ICollectionList;
import xyz.acrylicstyle.mcutil.lang.MCVersion;

import java.util.function.Function;

import static xyz.acrylicstyle.bw.BungeeWaiter.bool;

public class VersionsCommand extends Command {
    public VersionsCommand() {
        super("versions", "bungeewaiter.versions");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            CollectionList<String> messages = new CollectionList<>();
            ProxyServer.getInstance().getPlayers().forEach(player -> {
                MCVersion version = ICollectionList.asList(MCVersion.getByProtocolVersion(player.getPendingConnection().getVersion())).first();
                assert version != null;
                messages.add(ChatColor.YELLOW + player.getName() + ChatColor.AQUA + ": " + (version.isModern() ? ChatColor.GREEN : ChatColor.YELLOW) + version.getName() + "\n");
            });
            sender.sendMessage(messages.map((Function<String, TextComponent>) TextComponent::new).toArray(new TextComponent[0]));
        } else {
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(args[0]);
            if (player == null) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "That player is not online."));
                return;
            }
            MCVersion version = ICollectionList.asList(MCVersion.getByProtocolVersion(player.getPendingConnection().getVersion())).first();
            assert version != null;
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + player.getName() + ChatColor.AQUA + ":"));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + " - Version: " + ChatColor.AQUA + version.getName()));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + " - Modern(1.13+): " + bool(version.isModern())));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + " - Pre-release: " + bool(version.isPrerelease())));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + " - Snapshot: " + bool(version.isSnapshot())));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + " - Release Candidate: " + bool(version.isReleaseCandidate())));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + " - Protocol Version: " + ChatColor.RED + player.getPendingConnection().getVersion()));
        }
    }
}
