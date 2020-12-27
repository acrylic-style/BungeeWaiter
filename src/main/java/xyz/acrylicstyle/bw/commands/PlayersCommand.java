package xyz.acrylicstyle.bw.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import util.CollectionList;
import util.CollectionSet;
import util.ICollectionList;
import util.MultiCollection;
import xyz.acrylicstyle.bw.BungeeWaiter;
import xyz.acrylicstyle.mcutil.lang.MCVersion;

import java.util.Collections;
import java.util.function.Function;

public class PlayersCommand extends Command implements TabExecutor {
    public PlayersCommand() {
        super("players", "bungeewaiter.players");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "/players <byVersion/byLabel>"));
            return;
        }
        if (args[0].equalsIgnoreCase("byVersion")) {
            sendPlayersByVersion(sender);
        } else if (args[0].equalsIgnoreCase("byLabel")) {
            CollectionList<String> messages = new CollectionList<>();
            CollectionSet<String> labels = new CollectionSet<>();
            MultiCollection<String, String> players = new MultiCollection<>();
            ProxyServer.getInstance().getPlayers().forEach(player -> {
                String label = BungeeWaiter.getLabel(player);
                if (label == null) label = "<No Label>";
                labels.add(label);
                players.add(label, player.getName());
            });
            ICollectionList<String> versions2 = ICollectionList.asList(labels);
            versions2.forEach(label -> messages.add(ChatColor.LIGHT_PURPLE + "[" + label + ChatColor.LIGHT_PURPLE + "] " + ChatColor.YELLOW + "(" + ChatColor.RED + players.get(label).size() + ChatColor.YELLOW + ")" + ChatColor.WHITE + ": " + ChatColor.GREEN + players.get(label).join(ChatColor.YELLOW + ", " + ChatColor.GREEN)));
            messages.map((Function<String, TextComponent>) TextComponent::new).forEach(sender::sendMessage);
        } else {
            sender.sendMessage(new TextComponent(ChatColor.RED + "/players <byVersion/byLabel>"));
        }
    }

    static void sendPlayersByVersion(CommandSender sender) {
        CollectionList<String> messages = new CollectionList<>();
        CollectionSet<MCVersion> versions = new CollectionSet<>();
        MultiCollection<MCVersion, String> players = new MultiCollection<>();
        ProxyServer.getInstance().getPlayers().forEach(player -> {
            MCVersion v = BungeeWaiter.getReleaseVersionIfPossible(player.getPendingConnection().getVersion());
            versions.add(v);
            players.add(v, player.getName());
        });
        ICollectionList<MCVersion> versions2 = ICollectionList.asList(versions);
        versions2.sort((a, b) -> b.getProtocolVersion() - a.getProtocolVersion());
        versions2.forEach(version -> messages.add(ChatColor.LIGHT_PURPLE + "[" + (version.isModern() ? ChatColor.GREEN : ChatColor.YELLOW) + version.getName() + ChatColor.LIGHT_PURPLE + "] " + ChatColor.YELLOW + "(" + ChatColor.RED + players.get(version).size() + ChatColor.YELLOW + ")" + ChatColor.WHITE + ": " + ChatColor.GREEN + players.get(version).join(ChatColor.YELLOW + ", " + ChatColor.GREEN)));
        messages.map((Function<String, TextComponent>) TextComponent::new).forEach(sender::sendMessage);
    }

    private static final CollectionList<String> arguments = CollectionList.of("byVersion", "byLabel");

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 0) return arguments;
        if (args.length == 1) return ICollectionList.asList(arguments).filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()));
        return Collections.emptyList();
    }
}
