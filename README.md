# SyodogPE

SyodogPE is a fork of WaterdogPE with additional featues and fixes.
For example: You can receive all packets send by the users. You can use this to send sounds for example.
SyodogPE also fixes some bugs WaterdogPE still has.

WaterdogPE is a Minecraft: Bedrock Edition proxy software developed by the developers of the old Waterdog
Proxy.  
In this new proxy, we are working with Cloudbursts Protocol Library. It takes alot of maintaining effort from us and
provides us with a nice api to work with.  
If you want to discuss things without joining the discord server, please use the [Discussions Tab](https://github.com/WaterdogPE/WaterdogPE/discussions)

## Links
Waterdog Links! Some stuff may be different to SyodogPE
- [Website](https://waterdog.dev)
- [Discord](https://discord.gg/sJ452xNugw)
- [Official documentation](https://docs.waterdog.dev)
- [Issue Tracker](https://github.com/WaterdogPE/WaterdogPE/issues)
- [Public Plugins Page](https://plugins.waterdog.dev/)
- [Branding: various brand assets for the WaterdogPE project](https://github.com/WaterdogPE/Branding)

## Setup Guide
If you haven't used WaterdogPE before, we recommend you to take a look at our [setup guide](https://docs.waterdog.dev/books/waterdogpe-setup) in the docs.   
*Please note that the config from the old Waterdog (Bungee) is not compatible with WaterdogPE*

### Supported Software
Our focus lies on PowerNukkitX 2.0 since its the only server software that's worth using. (Besides Nukkit Mot maybe?)

You can find list of currently supported/unsupported software [here](https://docs.waterdog.dev/books/waterdogpe-setup/page/software-compatibility).

## Compiling

To compile SyodogPE please visit WaterdogPE's [COMPILING.md](COMPILING.md) guide.

## Maven usage

```xml
<repositories>
	<repository>
		<id>jitpack.io</id>
		<url>https://jitpack.io</url>
	</repository>
</repositories>

<dependencies>
	<dependency>
	    <groupId>com.github.Syodo-Development</groupId>
	    <artifactId>SyodogPE</artifactId>
	    <version>-SNAPSHOT</version>
	</dependency>
</dependencies>
```
