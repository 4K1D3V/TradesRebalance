package dev.oumaimaa.tradesrebalance.managers;

import dev.oumaimaa.tradesrebalance.TradesRebalance;
import dev.oumaimaa.tradesrebalance.config.ConfigurationManager;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTables;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class LootTableManager {
    private static final Map<LootTables, LootEnhancement> LOOT_ENHANCEMENTS = Map.ofEntries(
            Map.entry(LootTables.ANCIENT_CITY, new LootEnhancement(
                    Set.of(Material.ECHO_SHARD, Material.DISC_FRAGMENT_5),
                    0.15,
                    Set.of(Enchantment.SWIFT_SNEAK)
            )),
            Map.entry(LootTables.BASTION_TREASURE, new LootEnhancement(
                    Set.of(Material.NETHERITE_SCRAP, Material.ANCIENT_DEBRIS),
                    0.10,
                    Set.of(Enchantment.SOUL_SPEED)
            )),
            Map.entry(LootTables.BURIED_TREASURE, new LootEnhancement(
                    Set.of(Material.HEART_OF_THE_SEA, Material.TRIDENT),
                    0.20,
                    Set.of(Enchantment.MENDING)
            )),
            Map.entry(LootTables.DESERT_PYRAMID, new LootEnhancement(
                    Set.of(Material.DIAMOND, Material.EMERALD, Material.GOLDEN_APPLE),
                    0.12,
                    Set.of(Enchantment.FIRE_PROTECTION, Enchantment.THORNS)
            )),
            Map.entry(LootTables.END_CITY_TREASURE, new LootEnhancement(
                    Set.of(Material.ELYTRA, Material.DIAMOND, Material.EMERALD),
                    0.08,
                    Set.of(Enchantment.MENDING, Enchantment.UNBREAKING)
            )),
            Map.entry(LootTables.JUNGLE_TEMPLE, new LootEnhancement(
                    Set.of(Material.DIAMOND, Material.EMERALD),
                    0.15,
                    Set.of(Enchantment.FEATHER_FALLING, Enchantment.UNBREAKING)
            )),
            Map.entry(LootTables.NETHER_BRIDGE, new LootEnhancement(
                    Set.of(Material.DIAMOND, Material.GOLDEN_APPLE),
                    0.10,
                    Set.of(Enchantment.FIRE_ASPECT, Enchantment.BLAST_PROTECTION)
            )),
            Map.entry(LootTables.SHIPWRECK_TREASURE, new LootEnhancement(
                    Set.of(Material.DIAMOND, Material.EMERALD, Material.HEART_OF_THE_SEA),
                    0.18,
                    Set.of(Enchantment.DEPTH_STRIDER, Enchantment.RESPIRATION)
            )),
            Map.entry(LootTables.STRONGHOLD_LIBRARY, new LootEnhancement(
                    Set.of(Material.ENCHANTED_BOOK, Material.BOOK),
                    0.25,
                    Set.of(Enchantment.MENDING, Enchantment.SILK_TOUCH, Enchantment.FORTUNE)
            )),
            Map.entry(LootTables.UNDERWATER_RUIN_BIG, new LootEnhancement(
                    Set.of(Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE),
                    0.12,
                    Set.of(Enchantment.DEPTH_STRIDER, Enchantment.AQUA_AFFINITY)
            )),
            Map.entry(LootTables.WOODLAND_MANSION, new LootEnhancement(
                    Set.of(Material.DIAMOND, Material.ENCHANTED_GOLDEN_APPLE, Material.TOTEM_OF_UNDYING),
                    0.15,
                    Set.of(Enchantment.EFFICIENCY, Enchantment.SHARPNESS)
            )),
            Map.entry(LootTables.TRIAL_CHAMBERS_REWARD, new LootEnhancement(
                    Set.of(Material.DIAMOND, Material.EMERALD, Material.HEAVY_CORE),
                    0.20,
                    Set.of(Enchantment.BREACH, Enchantment.DENSITY, Enchantment.WIND_BURST)
            ))
    );

    private final TradesRebalance plugin;
    private final ConfigurationManager configManager;
    private final Map<LootTables, Boolean> modifiedTables = new ConcurrentHashMap<>();

    public LootTableManager(TradesRebalance plugin, ConfigurationManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void initialize() {
        if (!configManager.isLootTableUpdateEnabled()) {
            return;
        }

        plugin.getSLF4JLogger().info("Initializing loot table enhancements...");

        int enhanced = 0;
        for (var entry : LOOT_ENHANCEMENTS.entrySet()) {
            try {
                enhanceLootTable(entry.getKey(), entry.getValue());
                enhanced++;
            } catch (Exception e) {
                plugin.getSLF4JLogger().warn(
                        "Failed to enhance loot table: {}",
                        entry.getKey(),
                        e
                );
            }
        }

        plugin.getSLF4JLogger().info("Enhanced {} loot tables", enhanced);
    }

    private void enhanceLootTable(LootTables tableType, LootEnhancement enhancement) {
        // Note: Bukkit doesn't allow direct loot table modification
        // We'll track enhancements and apply them when loot is generated
        modifiedTables.put(tableType, true);

        if (configManager.isDebugMode()) {
            plugin.getSLF4JLogger().debug(
                    "Registered enhancement for loot table: {}",
                    tableType
            );
        }
    }

    public List<ItemStack> enhanceLoot(
            LootTables tableType,
            List<ItemStack> originalLoot,
            LootContext context) {

        if (!configManager.isLootTableUpdateEnabled()) {
            return originalLoot;
        }

        LootEnhancement enhancement = LOOT_ENHANCEMENTS.get(tableType);
        if (enhancement == null) {
            return originalLoot;
        }

        List<ItemStack> enhancedLoot = new ArrayList<>(originalLoot);

        // Add bonus items based on enhancement
        for (Material bonusItem : enhancement.bonusItems()) {
            if (shouldAddBonusItem(enhancement.chanceIncrease())) {
                enhancedLoot.add(createBonusItem(bonusItem, enhancement));
            }
        }

        // Enhance enchanted books with better enchantments
        enhancedLoot.replaceAll(item ->
                enhanceEnchantedBook(item, enhancement)
        );

        if (configManager.isDebugMode()) {
            plugin.getSLF4JLogger().debug(
                    "Enhanced loot from {}: {} -> {} items",
                    tableType,
                    originalLoot.size(),
                    enhancedLoot.size()
            );
        }

        return enhancedLoot;
    }

    private boolean shouldAddBonusItem(double chanceIncrease) {
        return Math.random() < chanceIncrease;
    }

    private @NotNull ItemStack createBonusItem(Material material, LootEnhancement enhancement) {
        ItemStack item = new ItemStack(material);

        // For enchanted books, add one of the enhancement enchantments
        if (material == Material.ENCHANTED_BOOK && !enhancement.priorityEnchantments().isEmpty()) {
            var enchants = new ArrayList<>(enhancement.priorityEnchantments());
            Enchantment selected = enchants.get(
                    new Random().nextInt(enchants.size())
            );

            var meta = item.getItemMeta();
            if (meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta storageMeta) {
                int level = Math.min(selected.getMaxLevel(),
                        Math.random() < 0.7 ? selected.getMaxLevel() : 1);
                storageMeta.addStoredEnchant(selected, level, true);
                item.setItemMeta(storageMeta);
            }
        }

        // Random quantity for stackable items
        if (item.getMaxStackSize() > 1) {
            int amount = switch (material) {
                case DIAMOND, EMERALD -> new Random().nextInt(1, 4);
                case GOLD_INGOT, IRON_INGOT -> new Random().nextInt(2, 9);
                default -> 1;
            };
            item.setAmount(amount);
        }

        return item;
    }

    @Contract("_, _ -> param1")
    private @NotNull ItemStack enhanceEnchantedBook(@NotNull ItemStack item, LootEnhancement enhancement) {
        if (item.getType() != Material.ENCHANTED_BOOK) {
            return item;
        }

        if (!(item.getItemMeta() instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta meta)) {
            return item;
        }

        // 30% chance to add a priority enchantment
        if (Math.random() < 0.3 && !enhancement.priorityEnchantments().isEmpty()) {
            var enchants = new ArrayList<>(enhancement.priorityEnchantments());
            Enchantment bonus = enchants.get(
                    new Random().nextInt(enchants.size())
            );

            if (!meta.hasStoredEnchant(bonus)) {
                int level = Math.min(bonus.getMaxLevel(),
                        Math.random() < 0.6 ? bonus.getMaxLevel() : 1);
                meta.addStoredEnchant(bonus, level, true);
                item.setItemMeta(meta);
            }
        }

        return item;
    }

    public boolean isLootTableEnhanced(LootTables tableType) {
        return modifiedTables.getOrDefault(tableType, false);
    }

    public @NotNull @UnmodifiableView Set<LootTables> getEnhancedTables() {
        return Collections.unmodifiableSet(modifiedTables.keySet());
    }


    public void reload() {
        modifiedTables.clear();
        initialize();
        plugin.getSLF4JLogger().info("Loot table manager reloaded");
    }

    public void shutdown() {
        modifiedTables.clear();
    }

    /**
     * Record class for loot table enhancements
     *
     * @param bonusItems           Items that have increased drop chance
     * @param chanceIncrease       Probability increase (0.0-1.0)
     * @param priorityEnchantments Enchantments to prioritize in books
     */
    public record LootEnhancement(
            Set<Material> bonusItems,
            double chanceIncrease,
            Set<Enchantment> priorityEnchantments
    ) {
    }
}