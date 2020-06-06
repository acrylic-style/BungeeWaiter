package xyz.acrylicstyle.bw;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class BungeeWaiter extends Plugin implements Listener {
    public static Logger LOGGER = Logger.getLogger("BungeeWaiter");
    public static Configuration config = null;
    public static String limbo = null;
    public static String target = null;
    public static boolean isTargetOnline = false;
    public static Map<UUID, TimerTask> tasks = new HashMap<>();

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
                            sender.sendMessage(new TextComponent(ChatColor.RED + "You don't have permission to do this."));
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
        getProxy().getPluginManager().registerListener(this, this);
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                ProxyServer.getInstance().getServerInfo(target).ping((ping, throwable) -> {
                    isTargetOnline = throwable == null;
                    if (isTargetOnline) {
                        getProxy().getPlayers().forEach(player -> {
                            if (player.getServer().getInfo().getName().equalsIgnoreCase(limbo)) player.connect(getProxy().getServerInfo(target));
                        });
                    }
                });
            }
        };
        timer.schedule(task, 5000, 5000);
    }

    @EventHandler
    public void onServerConnected(ServerConnectedEvent e) {
        TimerTask t = tasks.remove(e.getPlayer().getUniqueId());
        if (t != null) t.cancel();
        if (e.getServer().getInfo().getName().equalsIgnoreCase(limbo)) {
            Timer timer = new Timer();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    if (!e.getPlayer().isConnected()) this.cancel();
                    if (e.getPlayer().getServer().getInfo().getName().equalsIgnoreCase(limbo)) {
                        e.getPlayer().sendMessage(new TextComponent(ChatColor.YELLOW + "自動でサーバーに接続されます。そのままお待ちください。"));
                    } else this.cancel();
                }
            };
            tasks.put(e.getPlayer().getUniqueId(), task);
            timer.schedule(task, 0, 30000);
        }
    }
}
