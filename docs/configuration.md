Configuration, Permissions, Localization
========================================

Configuration
-------------
As usual with Bukkit plugins, the configuration is in the config.yml in the plugin's directory. A config.yml with the default settings is is created after starting and stopping the server with the plugin for the first time.

Please refer to the [default config.yml](https://github.com/MinecraftWars/Gringotts/blob/master/config.yml) for a complete example.

### Language ###
The `language` option allows you to set one of Gringotts' supported language for interaction messages with players. Currently supported options are `custom` (default/english) and `de` (German). See below on how to modify custom messages to your preferences.

### Currency ###
Per default Gringotts uses emeralds as currency, but this can be changed to any other type of item.

Example configuration section:

    currency:
      name:
        singular: Emerald
        plural: Emeralds
      digits: 2
      named-denominations: false
      denominations:
        - material: emerald
          value: 1
        - material: emerald_block
          value: 9

This is the default configuration which uses emeralds as currency, with emeralds having value 1, and emerald blocks value 9.

#### Individual settings

* `name` Name of the currency to be used in messages to players. Please enter both a singular and plural version.
* `digits` Decimal digits used in representation and calculation of the currency. Set this to 0 to use only whole number values.
* `named-denominations` Whether to display money values in messages split up over denomination values, or as a single sum.
* `denominations` A list of key-value mappings, defining the material and name of the item type to use as currency, and the value of the item. 


#### Denominations 

Denominations have the following format:
 
     denominations:
       - material: material name or id
         damage: damage value of material (optional)
         displayname: a custom name for the currency item (optional)
         lore: a list of custom item lore text lines (optional)
         value: a number
         unit-name: a name for the denomination for display in messages (optional)
         unit-name-plural: plural version of unit-name (optional)
       - (optional: more denominations)
       - ...
       
* `material` can be an item id, a name from the [material list](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html), or an item type parsable by Vault (see [Vault Items code](https://github.com/MilkBowl/VaultAPI/blob/master/src/main/java/net/milkbowl/vault/item/Items.java)), if Vault is installed.
* `damage` is the modifier value or subtypoe for an item type. For example, all the dyes have the same item type, but different damage values.
* `displayname` is a modified item name. Only items with this exact name, including colors, will count as the specified denomination. **This does *not* automatically rename all items of this type, you need to use a separate item renaming plugin for this.** It will, however, create currency items based on these settings.
* `lore` is a list of custom lore lines that have to be present for an item to be counted as currency. Like `displayname`, it will only work if added by a third party plugin
* `value` is a whole or fractional number denoting the value of a denomination. The number of fractional digits in a currency value should not exceed the number defined as `digits`.
* `unit-name` sets the name for the denomination in messages to the player, such as account balance. If not set, uses the item's display name or regular name.
* `unit-name-plural` is the plural version of `unit-name`. If not set, uses the singular unit name with 's' added.


##### Example denomination setup with different features used for each denomination

The following setup shows how to specify a currency with Lapis Lazuli as minor denomination with a value of 0.05, Skeleton Heads with a value of 10 and Creeper Heads renamed to "Danger Coin" and additional added lore with a value of 60.

    denominations:
      - material: ink_sack
        damage: 4
        value: 0.05
      - material: skull_item
        value: 10
      - material: skull_item
        damage: 4
        displayname: 'Danger Coin'
        lore: ['Awarded for stupidity in the face of danger','Handle with care']
        value: 60
        

### Taxes ###

Gringotts supports two types of taxes on transactions done via `/money pay` command: `flat` and `rate`. Flat taxes are a flat amount added to any transaction, while rate adds a percentage of the actual transaction. These can be used individually or combined.

Example configuration section:

    transactiontax:
      flat: 1.0
      rate: 0.05

This would add to every transaction 1 plus 5% of the transaction value. For instance, if you had issued the command `/money pay 200 notch` it would remove 211 emeralds from your account, and add 200 emeralds to notch's account.


### Misc ###

	startingbalance:
	  player: 0
	
Amount of virtual money to gift to players on first join, or accounts with other plugins upon creation. This money may be spent as usual, but will not be backed by physical currency. Enable these if you want your players. to start with some money that can't be lost or stolen.
  
---

    usevault:
      container: true
      enderchest: true

Globally enable use of specific kinds of vault:
* `container` Enable the use of container vaults: chests, dispensers and furnaces. If this is `false`, only player's inventory and/or enderchests will serve as a player "vault".
* `enderchest` Enable use of enderchest as vault for players globally. The permission `gringotts.usevault.enderchest` may still be used to disable this on a per-player/world basis.

---

    balance:
      show-inventory: true
      show-vault: true
      
Show or hide messages information in inventory and vault balance, in addition to total balance. Disable these if you'd like your balance messages to be less verbose.


Localization and message customization
--------------------------------------
On first start of Gringotts, a `messages.yml` file will be written to the Gringotts plugin folder.
You can freely edit the available strings and also include [text formatting (color) codes](http://minecraft.gamepedia.com/Formatting_codes).

Some messages contain variables, for example `%player` for the player's name or `%value` for the amount transferred in a transaction. It is important not to change or translate these variable names. They may only be used in messages where they are already present in the original version, but it is safe to omit them from a custom message.


Permissions
-----------

### Vault creation

    gringotts.createvault:
      default: true

Allow players to create any type of vault.

---

    gringotts.createvault.admin:
      default: op

Allow players to create vaults for other players.

---

    gringotts.createvault.player:
      default: true

Allow players to create vaults for their own account.

### Vault usage

    gringotts.usevault
      default: true

Use player inventory and player's enderchest as vault when player is online.

---

    gringotts.usevault.inventory
      default: true

Use player inventory as vault when player is online.

--- 
    gringotts.usevault.enderchest
      default: true

### User commands

Use Gringotts commands.

    gringotts.command
      default: true

---
 
Allow transfer command (pay)

    gringotts.transfer:
      default: true

---

Allow withdrawal of money from chest storage to inventory via `/money withdraw`.
    
    gringotts.command.withdraw:
      default: true
    
---

Allow deposit of money to chest storage from inventory via `/money deposit`.
    
    gringotts.command.deposit:
      default: true

### Admin permissions

Allow players to transfer money to other accounts via `/money pay`

    gringotts.admin:
      default: op

Allow use of all `/moneyadmin` commands
