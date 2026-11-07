# WaterdogPE
[![Build Status](https://github.com/WaterdogPE/WaterdogPE/actions/workflows/maven-build.yml/badge.svg)](https://github.com/WaterdogPE/WaterdogPE/actions?query=branch%3Amaster+is%3Acompleted+event%3Apush)
[![Discord Chat](https://img.shields.io/discord/767330242078834712.svg)](https://discord.gg/QcRRzXX)

WaterdogPE is a brand new Minecraft: Bedrock Edition proxy software developed by the developers of the old Waterdog
Proxy.  
In this new proxy, we are working with Cloudbursts Protocol Library. It takes alot of maintaining effort from us and
provides us with a nice api to work with.  
If you want to discuss things without joining the discord server, please use the [Discussions Tab](https://github.com/WaterdogPE/WaterdogPE/discussions)

## Links

- [Website](https://waterdog.dev)
- [Discord](https://discord.gg/sJ452xNugw)
- [Official documentation](https://docs.waterdog.dev)
- [Issue Tracker](https://github.com/WaterdogPE/WaterdogPE/issues)
- [Public Plugins Page](https://plugins.waterdog.dev/)
- [Branding: various brand assets for the WaterdogPE project](https://github.com/WaterdogPE/Branding)

## Setup Guide
If you haven't used WaterdogPE before, we recommend you to take a look at our [setup guide](https://docs.waterdog.dev/waterdogpe-setup/starting-waterdog) in the docs.   
*Please note that the config from the old Waterdog (Bungee) is not compatible with WaterdogPE*

### Supported Software
Our goal is to support all commonly used Minecraft: Bedrock server softwares. Spoons and unofficial forks will not be supported due to the lack
of proper implementation.  
You can find list of currently supported/unsupported software [here](https://docs.waterdog.dev/waterdogpe-setup/software-compatibility#software-compatibility).

## Compiling

To compile WaterdogPE please visit our [COMPILING.md](COMPILING.md) guide.

## Stable offline BDS identity

Modern BDS does not trust the XUID in a proxy-signed offline login. When the
client also leaves `SelfSignedId` blank, BDS has no stable key for player data
and treats every connection as a new player. This fork can derive a stable
fallback from the XUID that WaterdogPE already authenticated:

```yaml
stabilize_offline_identity: true
```

This does not make BDS trust or expose the XUID, so XUID-based operator records
still do not apply. It only keeps BDS's offline player-data identity stable.
Existing non-empty client `SelfSignedId` values are left unchanged.
## Maven usage

```xml
<repositories>
    <repository>
        <id>waterdog-repo</id>
        <url>https://repo.waterdog.dev/main</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>dev.waterdog.waterdogpe</groupId>
        <artifactId>waterdog</artifactId>
        <version>2.0.4-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

## Included libraries

- [Yamler: a forked yaml parsing library](https://github.com/WaterdogPE/Yamler)
- [Protocol: A fork of Nukkits Protocol library with various changes for the WaterdogPE project](https://github.com/WaterdogPE/Protocol)
