package xyz.acrylicstyle.bw;

public class KickData {
    private final String server;
    private final String message;

    public KickData(String server, String message) {
        this.server = server;
        this.message = message;
    }

    public String getServer() { return server; }

    public String getMessage() { return message; }
}
