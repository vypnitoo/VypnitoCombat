# VypnitoCombat

![Java Version](https://img.shields.io/badge/Java-17+-red.svg)
![Spigot API](https://img.shields.io/badge/API-1.21+-blue.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

A comprehensive combat management plugin designed to provide server owners with full control over PvP interactions, combat logging, and server safety.

---

## ✨ Features

- **Advanced Combat Tagging:** Puts players in combat mode upon attacking, preventing them from using disallowed commands or items.
- **Persistent Cooldowns:** Post-combat cooldowns for items like Elytras are saved to a database (SQLite) and persist through server restarts.
- **Global PvP Toggle:** Enable or disable PvP across the entire server with a simple command (`/pvp global <on|off>`).
- **Indirect PvP Protection:** When global PvP is off, players are protected from:
    - Lava placement near other players.
    - Harmful splash potions.
- **WorldGuard Integration:** Prevent players from entering "safe regions" (e.g., `pvp: deny` or `invincible: allow`) while in combat. Players are gently "bounced" back.
- **In-Combat Restrictions:**
    - Block specific commands (e.g., `/home`, `/spawn`).
    - Block specific items (e.g., Elytra).
    - Block usage of Riptide Tridents.
- **Admin Tools:** A full suite of commands for staff to manage the combat state of players (`/vc status`, `/vc tag`, `/vc untag`).
- **Customizable Punishments:** Configure what happens when a player combat logs - kill them, execute custom commands, or both.
- **PlaceholderAPI Support:** Placeholders to show a player's combat status in other plugins.
- **Highly Configurable:** Almost every feature, message, and timer can be configured via `config.yml` and `messages.yml`.

---

## 📋 Requirements

- **Java 17** or newer
- **Spigot/Paper 1.21+**
- **Optional Dependencies:**
    - `WorldGuard` (for region-based protections)
    - `PlaceholderAPI` (for placeholder support)

---

## 🛠️ Building from Source

1. Clone this repository.
2. Ensure you have Maven and JDK 17+ installed.
3. Run the command `mvn clean package` in the root directory.
4. The compiled JAR file will be in the `target` folder.

---

## 📄 License

This project is licensed under the **MIT License**. See the `LICENSE` file for details.
