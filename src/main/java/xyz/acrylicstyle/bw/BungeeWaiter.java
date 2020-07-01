package xyz.acrylicstyle.bw;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import util.CollectionList;
import util.ICollectionList;
import util.JSONAPI;
import util.StringCollection;
import xyz.acrylicstyle.bw.commands.TellCommand;
import xyz.acrylicstyle.bw.commands.VersionsCommand;
import xyz.acrylicstyle.mcutil.lang.MCVersion;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class BungeeWaiter extends Plugin implements Listener {
    public static final String PREFIX = ChatColor.GREEN + "[" + ChatColor.AQUA + "BungeeWaiter" + ChatColor.GREEN + "] " + ChatColor.YELLOW;
    public static Logger LOGGER = Logger.getLogger("BungeeWaiter");
    public static Configuration config = null;
    public static String limbo = null;
    public static String target = null;
    public static boolean isTargetOnline = false;
    public static Map<UUID, TimerTask> tasks = new HashMap<>();
    public static final List<UUID> notification = new ArrayList<>(); // invert

    @Override
    public void onEnable() {
        try {
            config = YamlConfiguration.getProvider(YamlConfiguration.class).load(new File("./plugins/BungeeWaiter/config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        config.getStringList("notification").forEach(s -> notification.add(UUID.fromString(s)));
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
        getProxy().getPluginManager().registerCommand(this, new TellCommand());
        getProxy().getPluginManager().registerCommand(this, new VersionsCommand());
        getProxy().getPluginManager().registerCommand(this, new Command("notification", "bungeewaiter.notification") {
            @Override
            public void execute(CommandSender sender, String[] args) {
                if (!(sender instanceof ProxiedPlayer)) return;
                ProxiedPlayer player = (ProxiedPlayer) sender;
                if (notification.contains(player.getUniqueId())) {
                    notification.remove(player.getUniqueId());
                    sender.sendMessage(new TextComponent(PREFIX + "Turned on the notification."));
                } else {
                    notification.add(player.getUniqueId());
                    sender.sendMessage(new TextComponent(PREFIX + "Turned off the notification."));
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

    @Override
    public void onDisable() {
        try {
            config.set("notification", ICollectionList.asList(notification).map(UUID::toString));
            YamlConfiguration.getProvider(YamlConfiguration.class).save(config, new File("./plugins/BungeeWaiter/config.yml"));
        } catch (IOException e) {
            LOGGER.warning("Failed to save config");
            e.printStackTrace();
        }
    }

    public static String bool(boolean bool) { return bool ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"; }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent e) {
        kickQueue.remove(e.getPlayer().getUniqueId());
        Server server = e.getPlayer().getServer();
        String name = server == null || server.getInfo() == null ? "Connect" : server.getInfo().getName();
        getProxy().getPlayers().forEach(player -> {
            if (player.hasPermission("bungeewaiter.logging") || player.hasPermission("bungeewaiter.notification")) {
                player.sendMessage(new TextComponent(PREFIX + e.getPlayer().getName() + ": " + name + " -> Disconnect"));
            }
        });
    }

    public static final CollectionList<UUID> kickQueue = new CollectionList<>();

    public StringCollection<String> getServerMap() {
        return new StringCollection<>(ICollectionList.asList(config.getStringList("servers")).toMap(s -> s.split(":")[0], s -> s.split(":")[1]).mapKeys((s, o) -> s.toLowerCase()));
    }

    @EventHandler
    public void onServerKick(ServerKickEvent e) {
        kickQueue.add(e.getPlayer().getUniqueId());
        if (e.getPlayer().getServer() == null) return;
        String currentServer = e.getPlayer().getServer().getInfo().getName();
        String target = getServerMap().get(currentServer.toLowerCase());
        if (target == null) return;
        e.setCancelled(true);
        e.setCancelServer(getProxy().getServerInfo(target));
        getProxy().getScheduler().schedule(this, () -> {
            if (e.getPlayer().getServer().getInfo().getName().equals(currentServer)) {
                e.getPlayer().connect(getProxy().getServerInfo(currentServer));
            }
        }, 5, TimeUnit.SECONDS);
    }

    public static final StringCollection<String> country = new StringCollection<>();

    @EventHandler
    public void onPreLogin(PreLoginEvent e) {
        if (config.getString("apiKey") == null) return;
        if (!(e.getConnection().getSocketAddress() instanceof InetSocketAddress)) return;
        String address = ((InetSocketAddress) e.getConnection().getSocketAddress()).getAddress().getHostAddress();
        JSONObject response = (JSONObject) new JSONAPI("http://api.ipstack.com/" + address + "?access_key=" + config.getString("apiKey")).call(JSONObject.class).getResponse();
        country.add(address, response.getString("country_code"));
    }

    @Nullable
    public String getCountry(@NotNull ProxiedPlayer player) {
        SocketAddress addr = player.getPendingConnection().getSocketAddress();
        if (!(addr instanceof InetSocketAddress)) return null;
        return country.get(((InetSocketAddress) addr).getAddress().getHostAddress());
    }

    @EventHandler
    public void onServerConnected(ServerConnectedEvent e) {
        boolean kicked = kickQueue.remove(e.getPlayer().getUniqueId());
        Server server = e.getPlayer().getServer();
        String name = server == null || server.getInfo() == null ? "Connect" : server.getInfo().getName();
        String target = e.getServer().getInfo().getName();
        String country = getCountry(e.getPlayer());
        if (country != null) country = ", " + country;
        if (country == null) country = "";
        String version = Objects.requireNonNull(ICollectionList.asList(MCVersion.getByProtocolVersion(e.getPlayer().getPendingConnection().getVersion())).first()).getName();
        TextComponent tc = new TextComponent(PREFIX + e.getPlayer().getName() + ChatColor.GRAY + "[" + version + country + "]"
                + ChatColor.YELLOW + ": " + name + " -> " + target + (kicked ? ChatColor.GRAY + " (kicked from " + name + ")" : ""));
        getProxy().getPlayers().forEach(player -> {
            if (player.hasPermission("bungeewaiter.logging") || player.hasPermission("bungeewaiter.notification")) {
                if (!notification.contains(player.getUniqueId())) {
                    player.sendMessage(tc);
                }
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
                if (isTargetOnline) {
                    if (e.getPlayer().getServer().getInfo().getName().equalsIgnoreCase(limbo)) {
                        e.getPlayer().connect(getProxy().getServerInfo(target));
                    }
                }
                if (e.getPlayer().getServer().getInfo().getName().equalsIgnoreCase(limbo)) {
                    e.getPlayer().sendMessage(new TextComponent(ChatColor.YELLOW + "自動でサーバーに接続されます。そのままお待ちください。"));
                } else this.cancel();
            }
        };
        tasks.put(e.getPlayer().getUniqueId(), task);
        timer.schedule(task, 100, 30000); // give a small delay
    }
}
