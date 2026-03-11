# LimitedMaces

**LimitedMaces** is a lightweight server-side plugin that gives server owners full control over how many **Maces** can exist on the server at any given time. Perfect for hardcore, SMP, or special-event servers where powerful items should stay rare and meaningful.

> 💡 Want a *one-mace server* like the Lifesteal SMP? Just set the mace limit to **1**!

---

## ✨ Features

- 🔢 **Global Mace Limit**  
  Set a hard cap on how many maces may exist across the entire server.

- 🧠 **Offline Player Tracking**  
  Maces are tracked even while players are offline — no loopholes.

- 🚫 **Container Restrictions**  
  Maces **cannot** be placed into:
  - Chests
  - Barrels
  - Shulker Boxes
  - Any other container

- ⚙️ **Crafting Control**
  - The mace crafting recipe is **disabled automatically** once the limit is reached
  - **Autocrafters cannot craft maces**
  - Players receive a clear message when the limit is reached

- 🧲 **Hopper Protection**
  - Hoppers cannot pick up maces

- 💥 **Destruction Handling**
  - When a mace is destroyed:
    - A **server-wide broadcast** is sent
    - A new mace can be crafted again (until the limit is reached)

- 🧹 **Illegal Mace Cleanup**
  - Untracked or duplicated maces are automatically removed

- ✨ **Optional Enchanting**
  - Control whether maces can be enchanted
  - When `allow-mace-enchanting` is **true**, maces can be placed into **Enchanting Tables** and **Anvils**
  - When **false**, maces cannot be placed into enchant tables or anvils, making them completely unenchantable

---

## 📁 Configuration

The configuration file is located at: plugins/LimitedMaces/config.yml

### 📝 Default / Example `config.yml`

```yml
version: 1.1.0

# Maximum number of maces allowed on the server
allowed-maces: 3

# Whether maces can be enchanted
# If false, maces cannot be placed into enchant tables or anvils
allow-mace-enchanting: true

messages:
  prefix: "&6[MultiMace]&r "
  crafted-broadcast: "&a%player% &7crafted &f%amount%&7 mace(s)! &7(%current%/%max%)"
  destroyed-broadcast: "&cA mace was destroyed! &7(last held by &f%lastHolder%&7) &7(%current%/%max%)"
  containers-blocked: "&cYou can't put the mace in any container."
  limit-reached: "&cMace limit reached. A mace must be destroyed before another can be crafted."
  illegal-removed: "&cAn illegal/untracked mace was removed."
  reload: "&aMultiMace config reloaded."
  no-permission: "&cYou don't have permission."
````

## 📦 Compatibility

* ✅ **Minecraft 1.21.x**
* 🚀 Designed to be lightweight and efficient
* 🧩 No dependencies required
