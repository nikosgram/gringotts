Usage
=====

Storing money in an account requires a Gringotts vault. A vault consists of a container, which can be either chest, dispenser or furnace, and a sign above or on it declaring it as a vault. A player may claim any number of vaults. Vaults are not protected from access through other players.

Vaults
------

Gringotts supports vaults for players. All vaults are created by placing a sign onto or above a container and writing the vault type on first line the sign. When a vault is successfully created, you will receive a message.

### Player vaults ###

First line: `[vault]`
Third line: will display your name on successful creation.

### Creating vaults for other players/accounts ###

The permission `createvault.forothers` allows you to create vaults for other players. that do not belong to you. To create a vault for others, write the appropriate vault type on the first line and the designated owner on the third line (player). 

Commands
--------

Arguments between `<>` (ex `<amount>`) are mandatory while ones between `[]` (ex. `[type]`) are optionals.

### User commands ###

| Command                        | Description                             | Aliases         |
| ------------------------------ | --------------------------------------- | --------------- |
| `/money`                       | Display your account's current balance. | `/m`, `/balance`|
| `/money pay <amount> <player>` | Pay an amount to a player. The transaction will only succeed if your account has at least the given amount plus any taxes that apply, and the receiving account has enough capacity for the amount. | none |
| `/money withdraw <amount>`     | Withdraw an amount from chest storage into inventory.| none |
| `/money deposit <amount>`      | Deposit an amount from inventory into chest storage. | none |

### Admin commands ###

| Command                                     | Description                                                                  | Aliases |
| ------------------------------------------- | ---------------------------------------------------------------------------- | ------- |
| `/moneyadmin b <account>`                   | Get the balance of a player's account.                                       | none    |
| `/moneyadmin add <amount> <account> [type]` | Add an amount of money to a player's account.                                | none    |
| `/moneyadmin rm <amount> <account> [type]`  | Remove an amount of money from a player's account.                           | none    |
| `/gringotts reload`                         | Reload Gringotts config.yml and messages.yml and apply any changed settings. | none    |
