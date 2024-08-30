Gringotts [![Java CI with Maven](https://github.com/nikosgram/gringotts/actions/workflows/maven.yml/badge.svg)](https://github.com/nikosgram/gringotts/actions/workflows/maven.yml) [![Maven Package](https://github.com/nikosgram/gringotts/actions/workflows/maven-publish.yml/badge.svg)](https://github.com/nikosgram/gringotts/actions/workflows/maven-publish.yml)
=========

Gringotts is an item-based economy plugin for the Spigot Minecraft server platform. Unlike earlier economy plugins, all
currency value and money transactions are based on actual items in Minecraft, per default emeralds. The goals are to add
a greater level of immersion, a generally more Minecraft-like feeling, and in the case of a PvP environment, making the
currency itself vulnerable to raiding.


### ⚠ Breaking Changes in Version 3.0.0

This version changed the way data are stored, so if you come from an older version of Gringotts, you need to manually update your database.

- Shutdown your server
- In your plugins folder, open the folder named `Gringotts`
- Make a copy of the file `Gringotts.db` in case something goes wrong during the update.
- Install sqlite3 command-line program, on linux you can do so by entering the following in the terminal `sudo apt install sqlite3`
(for windows refer to [this link](https://www.sqlite.org/cli.html))
- Open your terminal in Gringotts' directory and enter this command `sqlite3 Gringotts.db`
- In the shell enter the following instruction
```
CREATE TABLE db_migration (
  id                           integer not null,
  mchecksum                    integer not null,
  mtype                        varchar(1) not null,
  mversion                     varchar(150) not null,
  mcomment                     varchar(150) not null,
  mstatus                      varchar(10) not null,
  run_on                       timestamp not null,
  run_by                       varchar(30) not null,
  run_time                     integer not null,
  constraint pk_db_migration primary key (id)
);
```
Execute and then enter
`INSERT INTO db_migration VALUES(0,0,'I','0','<init>','SUCCESS',1721777343415,'foo',0);`
and
`INSERT INTO db_migration VALUES(1,-1450547331,'V','1.0','initial','SUCCESS',1721777343415,'foo',1);`

- If everything went smoothly you should be done, exit sqlite by tying `.exit`, replace gringotts.jar by the new version and start your server.

> You may get the error `Error: attempt to write a readonly database`, that means that your user doesn't have write permission for the file Gringotts.db.

> On the first launch you will get lot of `Balance differs for account` errors, they can be ignored. If you get one after that, please open an issue as it may indicate a money duplication glitch.


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
