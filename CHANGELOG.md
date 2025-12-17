# Changelog

## Version 1.1.1

### New Features
- Added detection for maces destroyed via `/kill` command or other forced removals
- Maces can no longer be taken from the creative menu item list - they must be crafted
- Added `/getuntrackedmace` command to give yourself an untracked mace that bypasses the mace limit
- Added `/clearuntrackedmaces` command to remove all untracked maces from the server
- Untracked maces are tracked the same way as regular maces but don't count toward the limit

### Bug Fixes
- Fixed maces being incorrectly counted as destroyed when picked up with your mouse cursor
- Fixed untracked maces broadcasting destruction messages to the entire server
- Fixed dropped untracked mace items not being removed when using `/clearuntrackedmaces`

### Technical Changes
- Improved item removal detection to distinguish between pickups, natural despawns, and forced removals
- Untracked maces are now silently removed without server-wide broadcasts

