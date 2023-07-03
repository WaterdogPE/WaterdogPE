# Draco

Draco is a fork of the popular Minecraft proxy software, WaterdogPE, specifically tailored for Minecraft: Bedrock Edition servers. This fork aims to bolster the security features of WaterdogPE by incorporating advanced firewall capabilities

### Supported Software
Our goal is to support all commonly used Minecraft: Bedrock server softwares. Spoons and unofficial forks will not be supported due to the lack
of proper implementation.  
You can find list of currently supported/unsupported software [here](https://docs.waterdog.dev/books/waterdogpe-setup/page/software-compatibility).

## Benefits compared to Waterdog

- Reduced memory usage
- Much cleaner Plugin API
- Much more configurable
- Easy to modify / contribute
- Active Support and quick updates for new MC:BE Versions
- BE server pinging: Easily ping your downstream server using a plugin to check its status
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
        <version>2.0.0-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

## Included libraries

- [Yamler: a forked yaml parsing library](https://github.com/WaterdogPE/Yamler)
- [Protocol: A fork of Nukkits Protocol library with various changes for the WaterdogPE project](https://github.com/WaterdogPE/Protocol)
