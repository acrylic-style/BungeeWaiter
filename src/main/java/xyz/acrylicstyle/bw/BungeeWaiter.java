package xyz.acrylicstyle.bw;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.logging.Logger;

public class BungeeWaiter extends Plugin implements Listener {
    public static final String PREFIX = ChatColor.GREEN + "[" + ChatColor.AQUA + "BungeeWaiter" + ChatColor.GREEN + "] " + ChatColor.YELLOW;
    public static Logger LOGGER = Logger.getLogger("BungeeWaiter");
    public static Configuration config = null;
    public static String limbo = null;
    public static String target = null;
    public static boolean isTargetOnline = false;
    public static Map<UUID, TimerTask> tasks = new HashMap<>();
    public static boolean notification = true;

    @Override
    public void onEnable() {
        try {
            config = YamlConfiguration.getProvider(YamlConfiguration.class).load(new File("./plugins/BungeeWaiter/config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        limbo = config.getString("limbo", "LIMBO");
        target = config.getString("target");
        if (target == null) {
            LOGGER.warning("Please specify target and limbo at plugins/BungeeWaiter/config.yml.");
            LOGGER.warning("Using the default value 'server' for target, LIMBO for limbo if undefined.");
            target = "server";
        }
        getProxy().getPluginManager().registerCommand(this, new Command("event") {
            @Override
            public void execute(CommandSender sender, String[] args) {
                if (args.length != 0 && args[0].equalsIgnoreCase("--force")) {
                    if (sender instanceof ProxiedPlayer) {
                        if (((ProxiedPlayer) sender).getServer().getInfo().getName().equalsIgnoreCase(target)) {
                            sender.sendMessage(new TextComponent(PREFIX + "You don't have permission to do this."));
                            return;
                        }
                    }
                }
                ProxyServer.getInstance().getServerInfo(target).ping((ping, throwable) -> {
                    isTargetOnline = throwable == null;
                    if (isTargetOnline) {
                        getProxy().getPlayers().forEach(player -> {
                            if (player.getServer().getInfo().getName().equalsIgnoreCase(limbo)) player.connect(getProxy().getServerInfo(target));
                        });
                    }
                });
            }
        });
        getProxy().getPluginManager().registerCommand(this, new Command("notification", "bungeewaiter.notification") {
            @Override
            public void execute(CommandSender sender, String[] args) {
                if (notification) {
                    notification = false;
                    sender.sendMessage(new TextComponent(PREFIX + "Turned off the notification for everyone."));
                } else {
                    notification = true;
                    sender.sendMessage(new TextComponent(PREFIX + "Turned on the notification for everyone."));
                }
            }
        });
        getProxy().getPluginManager().registerListener(this, this);
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                ProxyServer.getInstance().getServerInfo(target).ping((ping, throwable) -> {
                    isTargetOnline = throwable == null;
                    if (isTargetOnline) {
                        getProxy().getPlayers().forEach(player -> {
                            if (player.getServer().getInfo().getName().equalsIgnoreCase(limbo)) {
                                player.connect(getProxy().getServerInfo(target));
                            }
                        });
                    }
                });
            }
        };
        timer.schedule(task, 5000, 5000);
    }

    @EventHandler
    public void onServerConnected(ServerConnectedEvent e) {
        Server server = e.getPlayer().getServer();
        String name = server == null || server.getInfo() == null ? "<null>" : server.getInfo().getName();
        String target = e.getServer().getInfo().getName();
        getProxy().getPlayers().forEach(player -> {
            if (player.hasPermission("bungeewaiter.logging") || player.hasPermission("bungeewaiter.notification")) {
                player.sendMessage(new TextComponent(PREFIX + e.getPlayer().getName() + ": " + name + " -> " + target));
            }
        });
        TimerTask t = tasks.remove(e.getPlayer().getUniqueId());
        if (t != null) t.cancel();
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (!e.getPlayer().isConnected()) this.cancel();
                if (e.getPlayer().getServer() == null || e.getPlayer().getServer().getInfo() == null) {
                    LOGGER.warning(e.getPlayer().getName() + "'s server is null");
                    return;
                }
                if (e.getPlayer().getServer().getInfo().getName().equalsIgnoreCase(limbo)) {
                    if (notification)
                        e.getPlayer().sendMessage(new TextComponent(ChatColor.YELLOW + "自動でサーバーに接続されます。そのままお待ちください。"));
                } else this.cancel();
            }
        };
        tasks.put(e.getPlayer().getUniqueId(), task);
        timer.schedule(task, 100, 30000); // give a small delay
    }
}
