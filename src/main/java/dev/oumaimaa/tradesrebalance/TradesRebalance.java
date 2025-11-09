package dev.oumaimaa.tradesrebalance;

import dev.oumaimaa.tradesrebalance.commands.TradesRebalanceCommand;
import dev.oumaimaa.tradesrebalance.config.ConfigurationManager;
import dev.oumaimaa.tradesrebalance.listeners.VillagerTradeListener;
import dev.oumaimaa.tradesrebalance.listeners.WanderingTraderListener;
import dev.oumaimaa.tradesrebalance.managers.TradeManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Optional;

public final class TradesRebalance extends JavaPlugin {

    private static TradesRebalance instance;

    private ConfigurationManager configManager;
    private TradeManager tradeManager;
    private Logger logger;

    @Override
    public void onEnable() {
        instance = this;
        this.logger = getSLF4JLogger();
        initializeManagers();
        registerListeners();

        registerCommands();

        logger.info("TradesRebalance v{} has been enabled!", getPluginMeta().getVersion());
        logger.info("Replicating Minecraft 23w31a trade changes");
    }

    @Override
    public void onDisable() {
        Optional.ofNullable(tradeManager).ifPresent(TradeManager::shutdown);
        logger.info("TradesRebalance has been disabled!");
        instance = null;
    }

    private void initializeManagers() {
        try {
            this.configManager = new ConfigurationManager(this);
            this.tradeManager = new TradeManager(this, configManager);
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

    public static TradesRebalance getInstance() {
        return Objects.requireNonNull(instance, "Plugin instance is not initialized");
    }

    public ConfigurationManager getConfigManager() {
        return configManager;
    }

    public TradeManager getTradeManager() {
        return tradeManager;
    }

    public void reloadConfiguration() {
        try {
            configManager.reload();
            tradeManager.reload();
            logger.info("Configuration reloaded successfully");
        } catch (Exception e) {
            logger.error("Failed to reload configuration", e);
            throw new RuntimeException("Configuration reload failed", e);
        }
    }
}