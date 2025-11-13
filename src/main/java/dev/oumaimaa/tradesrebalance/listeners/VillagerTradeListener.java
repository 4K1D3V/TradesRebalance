package dev.oumaimaa.tradesrebalance.listeners;

import dev.oumaimaa.tradesrebalance.TradesRebalance;
import dev.oumaimaa.tradesrebalance.managers.TradeManager;
import dev.oumaimaa.tradesrebalance.trades.EnchantmentTradeFactory;
import org.bukkit.block.Biome;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class VillagerTradeListener implements Listener {

    private final TradesRebalance plugin;
    private final TradeManager tradeManager;
    private final EnchantmentTradeFactory enchantmentFactory;

    public VillagerTradeListener(TradesRebalance plugin, TradeManager tradeManager) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
        this.enchantmentFactory = new EnchantmentTradeFactory();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVillagerAcquireTrade(VillagerAcquireTradeEvent event) {
        if (!plugin.getConfigManager().isLibrarianNerfEnabled()) {
            return;
        }

        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }

        if (villager.getProfession() != Villager.Profession.LIBRARIAN) {
            return;
        }

        MerchantRecipe recipe = event.getRecipe();
        ItemStack result = recipe.getResult();

        if (!enchantmentFactory.isValidEnchantmentBook(result)) {
            return;
        }

        processEnchantmentTrade(event, villager, recipe, result);
    }

    private void processEnchantmentTrade(
            VillagerAcquireTradeEvent event,
            Villager villager,
            MerchantRecipe recipe,
            ItemStack result) {

        String biomeType = determineBiomeType(villager);

        Optional<Enchantment> currentEnchant = enchantmentFactory.getStoredEnchantment(result);
        if (currentEnchant.isEmpty()) {
            return;
        }

        var allowedEnchantments = plugin.getConfigManager().getBiomeEnchantments(biomeType);

        if (!allowedEnchantments.contains(currentEnchant.get())) {
            tradeManager.createBiomeSpecificEnchantmentTrade(
                    villager.getUniqueId(),
                    biomeType,
                    recipe
            ).ifPresent(event::setRecipe);

            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getSLF4JLogger().debug(
                        "Modified librarian trade in {} biome: {} -> new biome-specific enchantment",
                        biomeType,
                        currentEnchant.get().getKey()
                );
            }
        }
    }

    private @NotNull String determineBiomeType(@NotNull Villager villager) {
        Biome biome = villager.getLocation().getBlock().getBiome();
        String biomeName = biome.getKey().getKey().toUpperCase();

        return switch (biomeName) {
            case String s when s.contains("DESERT") -> "DESERT";
            case String s when s.contains("JUNGLE") -> "JUNGLE";
            case String s when s.contains("SAVANNA") -> "SAVANNA";
            case String s when s.contains("SNOWY") || s.contains("ICE") || s.contains("FROZEN") -> "SNOWY";
            case String s when s.contains("SWAMP") -> "SWAMP";
            case String s when s.contains("TAIGA") -> "TAIGA";
            default -> "PLAINS";
        };
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onVillagerCareerChange(@NotNull VillagerCareerChangeEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Villager villager = event.getEntity();

        // Clear cached trades when villager changes profession
        if (event.getReason() == VillagerCareerChangeEvent.ChangeReason.LOSING_JOB ||
                event.getReason() == VillagerCareerChangeEvent.ChangeReason.EMPLOYED) {

            CompletableFuture.runAsync(() ->
                    tradeManager.clearVillagerCache(villager.getUniqueId())
            );

            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getSLF4JLogger().debug(
                        "Cleared trade cache for villager {} due to career change: {}",
                        villager.getUniqueId(),
                        event.getReason()
                );
            }
        }
    }
}