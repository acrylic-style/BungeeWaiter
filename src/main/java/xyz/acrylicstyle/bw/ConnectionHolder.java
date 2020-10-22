package xyz.acrylicstyle.bw;

import org.jetbrains.annotations.NotNull;
import util.promise.Promise;
import xyz.acrylicstyle.sql.DataType;
import xyz.acrylicstyle.sql.Sequelize;
import xyz.acrylicstyle.sql.Table;
import xyz.acrylicstyle.sql.TableDefinition;
import xyz.acrylicstyle.sql.options.FindOptions;
import xyz.acrylicstyle.sql.options.UpsertOptions;

import java.sql.Driver;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Properties;

import static xyz.acrylicstyle.bw.BungeeWaiter.log;

public class ConnectionHolder extends Sequelize {
    public boolean connected = false;
    public Table ip;
    public Table lastIpV4;
    public Table lastIpV6;

    public ConnectionHolder(@NotNull String host, @NotNull String database, @NotNull String user, @NotNull String password) {
        super(host, database, user, password);
    }

    public void connect() {
        log.info("Connecting to database");
        Driver driver = null;
        try {
            driver = (Driver) Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
        } catch (ReflectiveOperationException ignore) {}
        if (driver == null) {
            try {
                driver = (Driver) Class.forName("com.mysql.jdbc.Driver").newInstance();
            } catch (ReflectiveOperationException ignore) {}
        }
        if (driver == null) throw new NoSuchElementException("Could not find any MySQL driver");
        Properties prop = new Properties();
        prop.setProperty("maxReconnects", "3");
        prop.setProperty("autoReconnect", "true");
        try {
            authenticate(driver, prop);
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        ip = this.define("ip", new TableDefinition[]{
                new TableDefinition.Builder("ip", DataType.STRING).setAllowNull(false).setPrimaryKey(true).build(),
                new TableDefinition.Builder("country", DataType.STRING).build(),
                new TableDefinition.Builder("country_updated_date", DataType.BIGINT).setAllowNull(false).setDefaultValue(0).build(),
        });
        lastIpV4 = this.define("lastIpV4", new TableDefinition[]{
                new TableDefinition.Builder("uuid", DataType.STRING).setAllowNull(false).setPrimaryKey(false).build(),
                new TableDefinition.Builder("name", DataType.STRING).build(),
                new TableDefinition.Builder("ip", DataType.STRING).setAllowNull(false).build(),
        });
        lastIpV6 = this.define("lastIpV6", new TableDefinition[]{
                new TableDefinition.Builder("uuid", DataType.STRING).setAllowNull(false).setPrimaryKey(false).build(),
                new TableDefinition.Builder("name", DataType.STRING).build(),
                new TableDefinition.Builder("ip", DataType.STRING).setAllowNull(false).build(),
        });
        try {
            this.sync();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        log.info("Successfully connected to database");
        connected = true;
    }

    public Promise<Void> setCountry(String ip, String country) {
        if (!connected) return Promise.getEmptyPromise();
        long time = System.currentTimeMillis();
        return this.ip.upsert(new UpsertOptions.Builder().addValue("ip", ip).addValue("country", country).addValue("country_updated_date", time).addWhere("ip", ip).build()).then(td -> null);
    }

    public Promise<String> getCountry(String ip) {
        if (!connected) return Promise.of(null);
        return this.ip.findOne(new FindOptions.Builder().addWhere("ip", ip).build()).then(tableData -> {
            if (tableData == null) return null;
            return tableData.getString("country");
        });
    }

    public Promise<Boolean> needsUpdate(String ip) {
        if (ip.startsWith("192.168.")) return Promise.of(false);
        if (!connected) return Promise.of(true);
        long time = System.currentTimeMillis();
        return this.ip.findOne(new FindOptions.Builder().addWhere("ip", ip).build()).then(tableData -> {
            if (tableData == null) return true;
            Long lastUpdated = tableData.getLong("country_updated_date");
            if (tableData.getString("country") == null || lastUpdated == null) return true;
            return time - lastUpdated > 2592000000L; /* a month */
        });
    }
}
