# WaterdogPE

WaterdogPE is a brand new Minecraft: Bedrock Edition proxy software developed by the developers
of the old Waterdog Proxy. 
<br>
In this new proxy, we are working with Cloudbursts Protocol Library. It takes alot of maintaining
effort from us and provides us with a nice api to work with.

## Links

- [Discord](https://discord.gg/sJ452xNugw)
- [Official documentation](https://docs.waterdog.dev)
- [Issues Tracker](https://github.com/WaterdogPE/WaterdogPE/issues)
- [Yamler: a forked yaml parsing library](https://github.com/WaterdogPE/Yamler)
- [Branding: various brand assets for the WaterdogPE project](https://github.com/WaterdogPE/Branding)
- [Protocol: A fork of Nukkits Protocol library with various changes for the WaterdogPE project](https://github.com/WaterdogPE/Protocol)

## If you want to discuss things without joining the discord server, please use the [Discussions Tab](https://github.com/WaterdogPE/WaterdogPE/discussions)

## Benefits compared to Waterdog 

- Reduced memory usage
- Much cleaner Plugin API
- Much more configurable
- Easy to modify / contribute
- Active Support and quick updates for new MC:BE Versions
- Dimension-less transfer: A transfer without any screen inbetween
- BE server pinging: Easily ping your downstream server using a plugin to check it's status
- Resource pack support
- Maintained documentation


## Known bugs fixed in WaterdogPE

- [Bows are sometimes not shooting when primed](https://github.com/yesdog/Waterdog/issues/53)
- [Scoreboards are flickering when updated](https://github.com/yesdog/Waterdog/issues/62)
- [Disconnects are not always showing the disconnect reason](https://github.com/yesdog/Waterdog/issues/97)
- [Waterdog is not working with PM4 due to appended custom data in the LoginPacket](https://github.com/yesdog/Waterdog/issues/161)
- [Resource packs not working properly](https://github.com/yesdog/Waterdog/issues/110)
- [Native ciphering sometimes fails](https://github.com/yesdog/Waterdog/issues/130)
- [Food bar not always working on Nukkit](https://github.com/yesdog/Waterdog/issues/144)

## Compiling
To compile WaterdogPE please visit our [COMPILING.md](COMPILING.md) guide.

## Maven usage
```mvn
    <repositories>
        <repository>
            <id>waterdog-repo</id>
            <url>https://repo.waterdog.dev/artifactory/main</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>dev.waterdog</groupId>
            <artifactId>waterdog</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
```
