# BungeeWaiter

## What's this
Wait at the lobby while you're waiting for server to open, then it will connect to the server automatically when server becomes online.

Pros:
- No login attacks when they're waiting for the server to open

Cons:
- Players will stuck at lobby server forever until they logouts if server never becomes open

## Downloads

\[[Download](https://um.acrylicstyle.xyz/35238159204/41167069/BungeeWaiter-1.0-SNAPSHOT.jar)]

## Configuration
`plugins/BungeeWaiter/config.yml`
```yaml
limbo: limbo # lobby server (players will stuck here while game server is offline)
target: game # game server (that will be transferred to)
```
