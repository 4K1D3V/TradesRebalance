package dev.oumaimaa.tradesrebalance.managers;

import dev.oumaimaa.tradesrebalance.TradesRebalance;
import dev.oumaimaa.tradesrebalance.config.ConfigurationManager;
import dev.oumaimaa.tradesrebalance.trades.EnchantmentTradeFactory;
import dev.oumaimaa.tradesrebalance.trades.WanderingTraderTradeFactory;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class TradeManager {

    private final TradesRebalance plugin;
    private final ConfigurationManager configManager;
    private final EnchantmentTradeFactory enchantmentFactory;
    private final WanderingTraderTradeFactory wanderingTraderFactory;

    private final Map<UUID, Set<Enchantment>> villagerTradeCache = new ConcurrentHashMap<>();
    private final Set<Material> commonItems;
    private final Set<Material> rareItems;

    public TradeManager(TradesRebalance plugin, ConfigurationManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.enchantmentFactory = new EnchantmentTradeFactory();
        this.wanderingTraderFactory = new WanderingTraderTradeFactory();

        this.commonItems = initializeCommonItems();
        this.rareItems = initializeRareItems();
    }

    private Set<Material> initializeCommonItems() {
        return EnumSet.of(
                Material.KELP,
                Material.SAND,
                Material.RED_SAND,
                Material.GRAVEL,
                Material.SUGAR_CANE,
                Material.VINE,
                Material.LILY_PAD,
                Material.CACTUS,
                Material.WHEAT_SEEDS,
                Material.PUMPKIN_SEEDS
        );
    }

    private Set<Material> initializeRareItems() {
        return EnumSet.of(
                Material.NAUTILUS_SHELL,
                Material.PODZOL,
                Material.PACKED_ICE,
                Material.BLUE_ICE,
                Material.TUBE_CORAL_BLOCK,
                Material.BRAIN_CORAL_BLOCK,
                Material.BUBBLE_CORAL_BLOCK,
                Material.FIRE_CORAL_BLOCK,
                Material.HORN_CORAL_BLOCK,
                Material.SLIME_BALL,
                Material.GUNPOWDER,
                Material.GLOWSTONE
        );
    }

    public Optional<MerchantRecipe> createBiomeSpecificEnchantmentTrade(
            UUID villagerId,
            String biomeType,
            MerchantRecipe originalRecipe) {

        Set<Enchantment> allowedEnchantments = configManager.getBiomeEnchantments(biomeType);

        if (allowedEnchantments.isEmpty()) {
            return Optional.empty();
        }

        // Get random enchantment from allowed set
        List<Enchantment> enchantmentList = new ArrayList<>(allowedEnchantments);
        Enchantment selectedEnchantment = enchantmentList.get(
                ThreadLocalRandom.current().nextInt(enchantmentList.size())
        );

        int level = calculateEnchantmentLevel(selectedEnchantment);

        // Cache the trade for this villager
        villagerTradeCache.computeIfAbsent(villagerId, k -> new HashSet<>())
                .add(selectedEnchantment);

        return enchantmentFactory.createEnchantmentTrade(
                selectedEnchantment,
                level,
                originalRecipe
        );
    }

    private int calculateEnchantmentLevel(@NotNull Enchantment enchantment) {
        int maxLevel = enchantment.getMaxLevel();
        if (maxLevel == 1) {
            return 1;
        }

        // 60% chance for max level, 40% for random lower level
        return ThreadLocalRandom.current().nextDouble() < 0.6
                ? maxLevel
                : ThreadLocalRandom.current().nextInt(1, maxLevel) + 1;
    }

    public List<MerchantRecipe> createWanderingTraderDyeTrades() {
        int count = configManager.getNewTradesCount();
        return wanderingTraderFactory.createDyeTrades(count);
    }

    public Optional<MerchantRecipe> adjustTradePrice(@NotNull MerchantRecipe recipe) {
        Material resultType = recipe.getResult().getType();

        if (configManager.shouldAdjustCommonPrices() && commonItems.contains(resultType)) {
            return wanderingTraderFactory.adjustPrice(recipe, -1);
        }

        if (configManager.shouldAdjustRarePrices() && rareItems.contains(resultType)) {
            return wanderingTraderFactory.adjustPrice(recipe, 1);
        }

        return Optional.of(recipe);
    }

    public boolean isCommonItem(Material material) {
        return commonItems.contains(material);
    }

    public boolean isRareItem(Material material) {
        return rareItems.contains(material);
    }

    public void clearVillagerCache(UUID villagerId) {
        villagerTradeCache.remove(villagerId);
    }

    public void clearAllCaches() {
        villagerTradeCache.clear();
    }

    public void reload() {
        clearAllCaches();
        plugin.getSLF4JLogger().info("Trade manager reloaded successfully");
    }

    public void shutdown() {
        clearAllCaches();
    }

    public Set<Enchantment> getCachedTrades(UUID villagerId) {
        return villagerTradeCache.getOrDefault(villagerId, Collections.emptySet());
    }
}