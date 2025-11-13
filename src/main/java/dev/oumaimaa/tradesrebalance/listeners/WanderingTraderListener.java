package dev.oumaimaa.tradesrebalance.listeners;

import dev.oumaimaa.tradesrebalance.TradesRebalance;
import dev.oumaimaa.tradesrebalance.managers.TradeManager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class WanderingTraderListener implements Listener {

    private final TradesRebalance plugin;
    private final TradeManager tradeManager;

    public WanderingTraderListener(TradesRebalance plugin, TradeManager tradeManager) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWanderingTraderSpawn(CreatureSpawnEvent event) {
        if (!plugin.getConfigManager().isWanderingTraderUpdateEnabled()) {
            return;
        }

        if (!(event.getEntity() instanceof WanderingTrader trader)) {
            return;
        }

        // Process trades asynchronously to avoid blocking the main thread
        CompletableFuture.runAsync(() -> scheduleTradeModification(trader))
                .exceptionally(throwable -> {
                    plugin.getSLF4JLogger().error(
                            "Failed to modify wandering trader trades",
                            throwable
                    );
                    return null;
                });
    }

    private void scheduleTradeModification(WanderingTrader trader) {
        // Schedule on main thread after trader is fully initialized
        plugin.getServer().getScheduler().runTask(plugin, () ->
                modifyTraderOffers(trader)
        );
    }

    private void modifyTraderOffers(WanderingTrader trader) {
        try {
            List<MerchantRecipe> currentRecipes = new ArrayList<>(trader.getRecipes());
            List<MerchantRecipe> modifiedRecipes = new ArrayList<>();

            // Update existing trades with price adjustments
            currentRecipes.stream()
                    .map(tradeManager::adjustTradePrice)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .forEach(modifiedRecipes::add);

            // Add new dye trades
            List<MerchantRecipe> dyeTrades = tradeManager.createWanderingTraderDyeTrades();
            modifiedRecipes.addAll(dyeTrades);

            // Apply modified trades to trader
            trader.setRecipes(modifiedRecipes);

            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getSLF4JLogger().debug(
                        "Modified wandering trader at {}: {} original trades, {} dye trades added, {} total",
                        trader.getLocation(),
                        currentRecipes.size(),
                        dyeTrades.size(),
                        modifiedRecipes.size()
                );
            }

        } catch (Exception e) {
            plugin.getSLF4JLogger().error(
                    "Failed to modify wandering trader offers at {}",
                    trader.getLocation(),
                    e
            );
        }
    }
}