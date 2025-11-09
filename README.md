# TradesRebalance v1.0

A modern, high-performance Minecraft plugin that replicates villager and wandering trader changes from Minecraft snapshot 23w31a. Built with **Java 21**, **Adventure API**, and advanced programming patterns for maximum efficiency and maintainability.

## ✨ Features

### 🔮 Librarian Nerf (Biome-Specific Enchantments)
Librarians now offer **biome-specific enchanted book trades**, encouraging exploration across different villages:

| Biome | Available Enchantments |
|-------|------------------------|
| 🏜️ **Desert** | Fire Protection, Thorns, Infinity |
| 🌴 **Jungle** | Feather Falling, Projectile Protection, Unbreaking |
| 🌾 **Plains** | Protection, Punch, Smite |
| 🦁 **Savanna** | Sharpness, Knockback, Binding Curse |
| ❄️ **Snowy** | Aqua Affinity, Looting, Frost Walker |
| 🐸 **Swamp** | Depth Strider, Respiration, Mending |
| 🌲 **Taiga** | Blast Protection, Fire Aspect, Flame |

### 🎒 Wandering Trader Updates
- All **16 dye colors** as new trades
- **Smart price adjustments** for common and rare items
- **Balanced trading economy** based on item rarity

## 🚀 Technical Features

### Modern Java 21 Features
- ✅ **Pattern Matching** with enhanced switch expressions
- ✅ **Record classes** for immutable data structures
- ✅ **Virtual threads** for async operations
- ✅ **Sequenced collections** for ordered data
- ✅ **Stream API** optimization
- ✅ **Optional** chaining for null safety

### Advanced Design Patterns
- 🏗️ **Factory Pattern** for trade generation
- 🎯 **Manager Pattern** for centralized logic
- 🔧 **Dependency Injection** principles
- 📦 **Separation of Concerns** architecture
- 🔄 **Async Processing** for non-blocking operations
- 💾 **Caching System** with ConcurrentHashMap

### Adventure API Integration
- 🎨 **Component-based messaging** (no deprecated ChatColor)
- 🌈 **Rich text formatting** with NamedTextColor
- ⚡ **Performance optimized** message building
- 🔒 **Type-safe** text components

## 📦 Installation

### Requirements
- **Java 21** or higher
- **Paper/Spigot 1.21+** (tested on 1.21.4)
- **Paper recommended** for best Adventure API support

### Steps
1. Download the latest release from [Releases](https://github.com/4K1D3V/TradesRebalance/releases)
2. Place `TradesRebalance-1.0.jar` in your `plugins` folder
3. Restart your server
4. Configure in `plugins/TradesRebalance/config.yml`

## 🛠️ Building from Source

### Prerequisites
- **JDK 21+** ([Download](https://adoptium.net/))
- **Maven 3.9+** ([Download](https://maven.apache.org/download.cgi))
- **Git** ([Download](https://git-scm.com/downloads))

### Build Commands
```bash
# Clone repository
git clone https://github.com/4K1D3V/TradesRebalance.git
cd TradesRebalance

# Build with Maven
mvn clean package

# Output: target/TradesRebalance-1.0.jar
```

### Development Build
```bash
# Compile without tests
mvn clean package -DskipTests

# Install to local Maven repository
mvn clean install
```

## 📁 Project Structure

```
src/main/java/me/yourname/tradesrebalance/
├── TradesRebalance.java                      # Main plugin class with DI
├── dev.oumaimaa.tradesrebalance.commands/
│   └── TradesRebalanceCommand.java           # Command handler with tab completion
├── config/
│   └── ConfigurationManager.java             # Configuration management
├── managers/
│   └── TradeManager.java                     # Centralized trade logic
├── trades/
│   ├── EnchantmentTradeFactory.java          # Enchantment trade generation
│   └── WanderingTraderTradeFactory.java      # Wandering trader trades
└── listeners/
    ├── VillagerTradeListener.java            # Villager trade events
    └── WanderingTraderListener.java          # Wandering trader events

src/main/resources/
├── plugin.yml                                 # Plugin metadata
└── config.yml                                 # User configuration
```

## 🎮 Commands

| Command | Description | Permission | Aliases |
|---------|-------------|------------|---------|
| `/tradesrebalance help` | Show help menu | `tradesrebalance.use` | `/tr`, `/tradesr` |
| `/tradesrebalance info` | Plugin information & status | `tradesrebalance.use` | |
| `/tradesrebalance reload` | Reload configuration | `tradesrebalance.reload` | |
| `/tradesrebalance debug <on\|off>` | Toggle debug mode | `tradesrebalance.reload` | |

### Command Examples
```
/tr info                    # Show plugin status
/tr reload                  # Reload config
/tr debug on                # Enable debug logging
```

## 🔐 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `tradesrebalance.use` | Basic command access | `true` |
| `tradesrebalance.reload` | Reload & debug dev.oumaimaa.tradesrebalance.commands | `op` |
| `tradesrebalance.admin` | All permissions | `op` |

## ⚙️ Configuration

### config.yml
```yaml
# Feature Toggles
enable-librarian-nerf: true
enable-wandering-trader-update: true

# Wandering Trader Settings
wandering-trader:
  new-trades-count: 2              # Number of dye trades to add (1-16)
  adjust-common-prices: true        # Make common items cheaper
  adjust-rare-prices: true          # Make rare items more expensive

# Librarian Settings
librarian:
  biome-enchantments:
    desert:
      - FIRE_PROTECTION
      - THORNS
      - INFINITY
    jungle:
      - FEATHER_FALLING
      - PROJECTILE_PROTECTION
      - UNBREAKING
    # ... more biomes

# Debug Mode
debug: false                        # Enable detailed console logging
```

### Configuration Hot-Reload
Changes can be applied without restart:
```
/tradesrebalance reload
```

## 🔧 Advanced Features

### Caching System
- Villager trades cached per UUID
- Automatic cache invalidation on profession change
- Thread-safe concurrent operations

### Async Processing
- Non-blocking trade modifications
- CompletableFuture for background tasks
- Main thread safety guaranteed

### Smart Price Calculation
```java
// Enchantment pricing algorithm
basePrice + (level * 5) + rarityBonus

// Treasure enchantments: 25 base
// Regular enchantments: 5-15 base
// Mending: +15 bonus
```

### Biome Detection
Uses modern Registry API for accurate biome identification:
- Pattern matching switch expressions
- Fuzzy matching for biome variants
- Fallback to Plains for unknown biomes

## 📊 Performance

- ⚡ **Zero lag** trade modifications
- 🚀 **Async processing** for heavy operations
- 💾 **Efficient caching** with ConcurrentHashMap
- 🎯 **Event priority** optimization
- 🔄 **Lazy initialization** where possible

## 🐛 Debugging

Enable debug mode for detailed logging:
```
/tradesrebalance debug on
```

Debug output includes:
- Trade modifications
- Biome detections
- Cache operations
- Price adjustments

## 🤝 Compatibility

| Software | Version | Status |
|----------|---------|--------|
| **Paper** | 1.21+ | ✅ Fully Supported |
| **Spigot** | 1.21+ | ✅ Supported |
| **Purpur** | 1.21+ | ✅ Compatible |
| **Bukkit** | 1.21+ | ⚠️ Limited (no Adventure) |
| **Java** | 21+ | ✅ Required |

## 📝 API Usage

### For Developers
```java
// Get plugin instance
TradesRebalance plugin = TradesRebalance.getInstance();

// Access managers
TradeManager tradeManager = plugin.getTradeManager();
ConfigurationManager configManager = plugin.getConfigManager();

// Create custom enchantment trade
EnchantmentTradeFactory factory = new EnchantmentTradeFactory();
Optional<MerchantRecipe> trade = factory.createEnchantmentTrade(
    Enchantment.MENDING,
    1,
    originalRecipe
);

// Check if item is rare
boolean isRare = tradeManager.isRareItem(Material.BLUE_ICE);
```

## 🔄 Migration from v2.x

Version 1.0 introduces breaking changes:

1. **Java 21 required** (was Java 17)
2. **Adventure API** (no more ChatColor)
3. **New config structure** (backup your config)
4. **Paper recommended** (better Adventure support)

## 📄 License

This project is licensed under the **MIT License** - see [LICENSE](LICENSE) for details.

## 👏 Credits

- **Original Concept**: [Agus5534](https://github.com/Agus5534/TradesRebalance)
- **Modern Rewrite**: Updated for Minecraft 1.21+ with Java 21 & Adventure API
- **Minecraft**: Mojang Studios
- **Bukkit/Spigot/Paper**: SpigotMC team

## 📮 Support

- 🐛 **Bug Reports**: [GitHub Issues](https://github.com/4K1D3V/TradesRebalance/issues)
- 💡 **Feature Requests**: [GitHub Discussions](https://github.com/4K1D3V/TradesRebalance/discussions)
- 📖 **Documentation**: [Wiki](https://github.com/you4K1D3Vrusername/TradesRebalance/wiki)
- 💬 **Discord**: [Join Server](https://discord.gg/4K1D3V)

## 📈 Changelog

### Version 1.0 (Current)
- ✨ Complete rewrite using Java 21 features
- 🎨 Adventure API integration (no deprecated methods)
- 🏗️ Advanced design patterns (Factory, Manager, DI)
- ⚡ Async processing for performance
- 💾 Concurrent caching system
- 🔧 Enhanced configuration management
- 🐛 Debug mode with detailed logging
- 📝 Tab completion for all dev.oumaimaa.tradesrebalance.commands
- 🎯 Pattern matching & modern Java syntax

### Version 2.0.0
- Initial modern version for 1.21+
- Basic biome enchantments
- Wandering trader dye trades

---

**Made with ❤️ for the Minecraft community**