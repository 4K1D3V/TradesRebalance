package dev.oumaimaa.tradesrebalance;

import dev.oumaimaa.tradesrebalance.commands.TradesRebalanceCommand;
import dev.oumaimaa.tradesrebalance.config.ConfigurationManager;
import dev.oumaimaa.tradesrebalance.listeners.*;
import dev.oumaimaa.tradesrebalance.managers.LootTableManager;
import dev.oumaimaa.tradesrebalance.managers.TradeManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Optional;

public final class TradesRebalance extends JavaPlugin {

    private static TradesRebalance instance;

    private ConfigurationManager configManager;
    private TradeManager tradeManager;
    private LootTableManager lootTableManager;
    private Logger logger;

    public static TradesRebalance getInstance() {
        return Objects.requireNonNull(instance, "Plugin instance is not initialized");
    }

    @Override
    public void onEnable() {
        instance = this;
        this.logger = getSLF4JLogger();

        initializeManagers();
        registerListeners();
        registerCommands();

        logger.info("TradesRebalance v{} has been enabled!", getPluginMeta().getVersion());
        logger.info("Replicating Minecraft 23w31a trade changes + Extended features");
        logEnabledFeatures();
    }

    @Override
    public void onDisable() {
        // Cleanup operations
        Optional.ofNullable(tradeManager).ifPresent(TradeManager::shutdown);
        Optional.ofNullable(lootTableManager).ifPresent(LootTableManager::shutdown);

        logger.info("TradesRebalance has been disabled!");
        instance = null;
    }

    private void initializeManagers() {
        try {
            this.configManager = new ConfigurationManager(this);
            this.tradeManager = new TradeManager(this, configManager);
            this.lootTableManager = new LootTableManager(this, configManager);

            lootTableManager.initialize();

            logger.info("Successfully initialized all managers");
        } catch (Exception e) {
            logger.error("Failed to initialize managers", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void registerListeners() {
        var pluginManager = getServer().getPluginManager();

        pluginManager.registerEvents(new VillagerTradeListener(this, tradeManager), this);
        pluginManager.registerEvents(new WanderingTraderListener(this, tradeManager), this);
        pluginManager.registerEvents(new CartographerTradeListener(this, tradeManager), this);
        pluginManager.registerEvents(new ArmorerTradeListener(this, tradeManager), this);
        pluginManager.registerEvents(new ToolsmithTradeListener(this, tradeManager), this);
        pluginManager.registerEvents(new WeaponsmithTradeListener(this, tradeManager), this);
        pluginManager.registerEvents(new LootTableListener(this, lootTableManager), this);

        logger.info("Successfully registered all event listeners");
    }

    private void registerCommands() {
        Optional.ofNullable(getCommand("tradesrebalance"))
                .ifPresentOrElse(
                        cmd -> {
                            var executor = new TradesRebalanceCommand(this, configManager);
                            cmd.setExecutor(executor);
                            cmd.setTabCompleter(executor);
                            logger.info("Successfully registered command executor");
                        },
                        () -> logger.warn("Failed to register command - command not found in plugin.yml")
                );
    }

    public ConfigurationManager getConfigManager() {
        return configManager;
    }

    public TradeManager getTradeManager() {
        return tradeManager;
    }

    public LootTableManager getLootTableManager() {
        return lootTableManager;
    }

    public void reloadConfiguration() {
        try {
            configManager.reload();
            tradeManager.reload();
            lootTableManager.reload();
            logger.info("Configuration reloaded successfully");
        } catch (Exception e) {
            logger.error("Failed to reload configuration", e);
            throw new RuntimeException("Configuration reload failed", e);
        }
    }

    private void logEnabledFeatures() {
        logger.info("Enabled features:");
        if (configManager.isLibrarianNerfEnabled())
            logger.info("  ✓ Librarian biome-specific enchantments");
        if (configManager.isWanderingTraderUpdateEnabled())
            logger.info("  ✓ Wandering trader dye trades");
        if (configManager.isCartographerUpdateEnabled())
            logger.info("  ✓ Cartographer map adjustments");
        if (configManager.isArmorerUpdateEnabled())
            logger.info("  ✓ Armorer diamond & chainmail rebalance");
        if (configManager.isToolsmithUpdateEnabled())
            logger.info("  ✓ Toolsmith enchanted tools");
        if (configManager.isWeaponsmithUpdateEnabled())
            logger.info("  ✓ Weaponsmith enhanced weapons");
        if (configManager.isLootTableUpdateEnabled())
            logger.info("  ✓ Loot table enhancements");
    }
}