package xyz.acrylicstyle.bw;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
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
import util.Collection;
import util.CollectionList;
import util.ICollectionList;
import util.JSONAPI;
import util.StringCollection;
import util.promise.Promise;
import xyz.acrylicstyle.bw.commands.GKickCommand;
import xyz.acrylicstyle.bw.commands.PingAllCommand;
import xyz.acrylicstyle.bw.commands.PingCommand;
import xyz.acrylicstyle.bw.commands.SAlertCommand;
import xyz.acrylicstyle.bw.commands.TellCommand;
import xyz.acrylicstyle.bw.commands.VersionsCommand;
import xyz.acrylicstyle.mcutil.lang.MCVersion;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
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
    public static Logger log;
    public static Configuration config = null;
    public static String limbo = null;
    public static String target = null;
    public static boolean isTargetOnline = false;
    public static Map<UUID, TimerTask> tasks = new HashMap<>();
    public static final List<UUID> notification = new ArrayList<>(); // invert
    public static CollectionList<UUID> noWarp = new CollectionList<>();
    public static final Timer timer = new Timer();
    public static ConnectionHolder db;

    @Override
    public void onEnable() {
        log = getLogger();
        try {
            config = YamlConfiguration.getProvider(YamlConfiguration.class).load(new File("./plugins/BungeeWaiter/config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String host = config.getString("database.host");
        String name = config.getString("database.name");
        String user = config.getString("database.user");
        String password = config.getString("database.password");
        if (host == null || name == null || user == null || password == null) {
            log.info("One of database settings is null, not using database.");
        } else {
            db = new ConnectionHolder(host, name, user, password);
            db.connect();
        }
        config.getStringList("notification").forEach(s -> notification.add(UUID.fromString(s)));
        limbo = config.getString("limbo", "LIMBO");
        target = config.getString("target");
        noWarp = ICollectionList.asList(config.getStringList("nowarp")).map(UUID::fromString);
        if (target == null) {
            log.warning("Please specify target and limbo at plugins/BungeeWaiter/config.yml.");
            log.warning("Using the default value 'server' for target, LIMBO for limbo if undefined.");
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
                            if (noWarp.contains(player.getUniqueId())) return;
                            if (player.getServer().getInfo().getName().equalsIgnoreCase(limbo)) player.connect(getProxy().getServerInfo(target));
                        });
                    }
                });
            }
        });
        getProxy().getPluginManager().registerCommand(this, new GKickCommand());
        getProxy().getPluginManager().registerCommand(this, new TellCommand());
        getProxy().getPluginManager().registerCommand(this, new VersionsCommand());
        getProxy().getPluginManager().registerCommand(this, new SAlertCommand());
        getProxy().getPluginManager().registerCommand(this, new PingCommand());
        getProxy().getPluginManager().registerCommand(this, new PingAllCommand());
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
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                ServerInfo info = ProxyServer.getInstance().getServerInfo(target);
                if (info == null) {
                    log.warning("Could not get server info for " + target);
                    return;
                }
                info.ping((ping, throwable) -> {
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
        timer.schedule(task, 5000*2, 5000*2);
    }

    @Override
    public void onDisable() {
        try {
            config.set("notification", ICollectionList.asList(notification).map(UUID::toString));
            YamlConfiguration.getProvider(YamlConfiguration.class).save(config, new File("./plugins/BungeeWaiter/config.yml"));
        } catch (IOException e) {
            log.warning("Failed to save config");
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

    public static final Collection<UUID, String> kickQueue = new Collection<>();

    public StringCollection<String> getServerMap() {
        return new StringCollection<>(ICollectionList.asList(config.getStringList("servers")).toMap(s -> s.split(":")[0], s -> s.split(":")[1]).mapKeys((s, o) -> s.toLowerCase()));
    }

    @EventHandler
    public void onServerKick(ServerKickEvent e) {
        kickQueue.add(e.getPlayer().getUniqueId(), TextComponent.toLegacyText(e.getKickReasonComponent()));
        if (e.getPlayer().getServer() == null) return;
        e.getPlayer().sendMessage(new TextComponent(ChatColor.RED + "サーバーからキックされました:"));
        e.getPlayer().sendMessage(e.getKickReasonComponent());
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

    @EventHandler
    public void onPreLogin(PreLoginEvent e) {
        if (config.getString("apiKey") == null) return;
        if (!(e.getConnection().getSocketAddress() instanceof InetSocketAddress)) return;
        try {
            String address = ((InetSocketAddress) e.getConnection().getSocketAddress()).getAddress().getHostAddress();
            db.needsUpdate(address).then(update -> {
                if (!update) return null; // if it doesn't needs to update country data, return
                JSONObject response = new JSONAPI("http://api.ipstack.com/" + address + "?access_key=" + config.getString("apiKey")).call(JSONObject.class).getResponse();
                db.setCountry(address, response.getString("country_code")).queue();
                return null;
            }).queue();
        } catch (RuntimeException ignored) {}
    }

    @NotNull
    public Promise<@Nullable String> getCountry(@NotNull ProxiedPlayer player) {
        SocketAddress addr = player.getPendingConnection().getSocketAddress();
        if (!(addr instanceof InetSocketAddress)) return Promise.of(null);
        return db.getCountry(((InetSocketAddress) addr).getAddress().getHostAddress());
    }

    public static MCVersion getReleaseVersionIfPossible(int protocolVersion) {
        CollectionList<MCVersion> list = ICollectionList.asList(MCVersion.getByProtocolVersion(protocolVersion));
        return list.filter(v -> !v.isSnapshot()).size() == 0 // if non-snapshot version wasn't found
                ? Objects.requireNonNull(list.first()) // return the last version anyway
                : Objects.requireNonNull(list.filter(v -> !v.isSnapshot()).first()); // return non-snapshot version instead
    }

    @EventHandler
    public void onServerConnected(ServerConnectedEvent e) {
        String kickMessage = kickQueue.remove(e.getPlayer().getUniqueId());
        Server server = e.getPlayer().getServer();
        String name = server == null || server.getInfo() == null ? "Connect" : server.getInfo().getName();
        if (server == null || server.getInfo() == null) kickMessage = null;
        String target = e.getServer().getInfo().getName();
        String country = getCountry(e.getPlayer()).complete();
        if (country != null) country = ", " + country;
        if (country == null) country = "";
        String version = getReleaseVersionIfPossible(e.getPlayer().getPendingConnection().getVersion()).getName();
        SocketAddress address = e.getPlayer().getPendingConnection().getSocketAddress();
        String iptype = "";
        if (address instanceof InetSocketAddress) {
            InetAddress addr = ((InetSocketAddress) address).getAddress();
            if (addr instanceof Inet6Address) {
                iptype = ", IPv6";
            } else if (addr instanceof Inet4Address) {
                iptype = ", IPv4";
            }
        }
        TextComponent tc = new TextComponent(PREFIX + e.getPlayer().getName() + ChatColor.GRAY + "[" + version + iptype + country + "]"
                + ChatColor.YELLOW + ": " + name + " -> " + target + (kickMessage != null ? ChatColor.GRAY + " (kicked from " + name + ": " + kickMessage + ")" : ""));
        getProxy().getPlayers().forEach(player -> {
            if (player.hasPermission("bungeewaiter.logging") || player.hasPermission("bungeewaiter.notification")) {
                if (!notification.contains(player.getUniqueId())) {
                    player.sendMessage(tc);
                }
            }
        });
        TimerTask t = tasks.remove(e.getPlayer().getUniqueId());
        if (t != null) t.cancel();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (!e.getPlayer().isConnected()) this.cancel();
                if (e.getPlayer().getServer() == null || e.getPlayer().getServer().getInfo() == null) {
                    log.warning(e.getPlayer().getName() + "'s server is null");
                    return;
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
