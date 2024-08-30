Gringotts [![Java CI with Maven](https://github.com/nikosgram/gringotts/actions/workflows/maven.yml/badge.svg)](https://github.com/nikosgram/gringotts/actions/workflows/maven.yml) [![Maven Package](https://github.com/nikosgram/gringotts/actions/workflows/maven-publish.yml/badge.svg)](https://github.com/nikosgram/gringotts/actions/workflows/maven-publish.yml)
=========

Gringotts is an item-based economy plugin for the Spigot Minecraft server platform. Unlike earlier economy plugins, all
currency value and money transactions are based on actual items in Minecraft, per default emeralds. The goals are to add
a greater level of immersion, a generally more Minecraft-like feeling, and in the case of a PvP environment, making the
currency itself vulnerable to raiding.

### Get Gringotts

- [from Spigot](https://www.spigotmc.org/resources/gringotts.42071/)
- [from Hangar](https://hangar.papermc.io/nikosgram/Gringotts)

Features
--------

* Item-backed economy (configurable, default emeralds)
* Multiple denominations with automatic conversion (for example, use emeralds and emerald blocks)
* Storage of currency in chests and other containers, player inventory and ender chests (configurable)
* Direct account-to-account transfers commands
* Optional transaction taxes
* Fractional currency values (fixed decimal digits)
* [Vault](https://www.spigotmc.org/resources/vault.34315/) integration

Usage
-----
Storing money in an account requires a Gringotts vault. A vault consists of a container, which can be either chest,
dispenser or furnace, and a sign above or on it declaring it as a vault. A player may claim any number of
vaults. Vaults are not protected from access through other players. If you would like them to be, you may use additional
plugins such as [LWC](https://dev.bukkit.org/projects/lwc/) or [WorldGuard](https://dev.bukkit.org/projects/worldguard/)
.

[Read how to use gringotts](https://github.com/nikosgram/Gringotts/wiki/Usage).

Configuration
-----
Read [how to config gringotts](https://github.com/nikosgram/Gringotts/wiki/Configuration).

Permissions
-----
Read [how gringotts permissions works](https://github.com/nikosgram/Gringotts/wiki/Permissions).

Commands
--------
Read [how to use gringotts commands](https://github.com/nikosgram/Gringotts/wiki/Commands).

Installation and Configuration
------------------------------
Download [Gringotts](https://www.spigotmc.org/resources/gringotts.42071/) and place it in your craftbukkit/plugins
folder

Please see the [Configuration](https://github.com/nikosgram/Gringotts/wiki/Permissions)
and [Permissions](https://github.com/nikosgram/Gringotts/wiki/Permissions) document on how to configure Gringotts.

Problems? Questions?
--------------------
Have a look at the [Wiki](https://github.com/nikosgram/Gringotts/wiki). You're welcome to improve it, too!

Development
-----------
Would you like to make changes to Gringotts yourself? Fork it!
Pull requests are very welcome, but please make sure your changes fulfill the Gringotts quality baseline:

* new features, settings, permissions are documented
* required dependencies are all added to the build by Maven, not included in the repo
* the project builds with Maven out-of-the-box

Gringotts uses the [Maven 3](http://maven.apache.org/) build system. Build a working plugin jar with the command

```shell
mvn compile install
```

Metrics
-------
[![Gringotts Metrics](https://bstats.org/signatures/bukkit/Gringotts.svg)](https://bstats.org/plugin/bukkit/Gringotts/4998)

## Contributors

<a href="https://github.com/nikosgram/gringotts/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=nikosgram/gringotts"  alt="Gringotts contributors"/>
</a>

Made with [contrib.rocks](https://contrib.rocks/preview?repo=nikosgram%2Fgringotts)

License
-------
All code within Gringotts is licensed under the BSD 2-clause license. See `license.txt` for details.
