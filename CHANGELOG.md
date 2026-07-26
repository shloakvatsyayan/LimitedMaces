# Changelog

## Version 1.2.0
- Added Minecraft Java Edition 26.x compatibility validation through the configurable Spigot API target
- Documented support for Minecraft 1.21.x and Java Edition 26.x
- Made mace crafting atomic so registered mace counts cannot diverge from delivered tagged maces

## Version 1.1.5
- **New config option**: `block-container-storage` - When set to `true` (default), prevents maces from being stored in containers. Set to `false` to allow storing maces in chests, hoppers, etc.
- **Config auto-update**: Existing configs will automatically receive the new option when upgrading
