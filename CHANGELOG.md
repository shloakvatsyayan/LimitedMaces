# Changelog

## Version 1.1.4
- **Renamed plugin** from "MultiMace" to "LimitedMaces"
- **Renamed package** from `org.bonkmc.multiMace` to `org.bonkmc.limitedmaces`
- **New folder structure**: Config and data files now stored directly in `plugins/LimitedMaces/` instead of subfolders
  - `config.yml` is now at `plugins/LimitedMaces/config.yml`
  - `maces.yml` is now at `plugins/LimitedMaces/maces.yml`
- **New commands**:
  - `/maces setlimit <n>` - Change the mace limit in-game
  - `/maces enchanting <on|off>` - Toggle mace enchanting in-game
  - `/removemace <id>` - Remove a specific mace from the plugin (also removes item from players, containers, and dropped items)
- **New permissions**:
  - `limitedmaces.setlimit` - Permission to change mace limit
  - `limitedmaces.enchanting` - Permission to toggle enchanting
  - `limitedmaces.remove` - Permission to remove maces
- **Automatic migration**: Servers upgrading from 1.1.3 or earlier will have their config and data automatically migrated from the old `MultiMace` folder
- All permissions renamed from `multimace.*` to `limitedmaces.*`

