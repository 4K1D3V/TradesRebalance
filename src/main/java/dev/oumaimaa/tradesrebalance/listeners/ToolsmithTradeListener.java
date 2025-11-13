package dev.oumaimaa.tradesrebalance.listeners;

import dev.oumaimaa.tradesrebalance.TradesRebalance;
import dev.oumaimaa.tradesrebalance.managers.TradeManager;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class ToolsmithTradeListener implements Listener {

    private static final Map<Material, Set<Enchantment>> DIAMOND_TOOL_ENCHANTS = Map.of(
            Material.DIAMOND_PICKAXE, Set.of(
                    Enchantment.EFFICIENCY,
                    Enchantment.FORTUNE,
                    Enchantment.SILK_TOUCH,
                    Enchantment.UNBREAKING
            ),
            Material.DIAMOND_AXE, Set.of(
                    Enchantment.EFFICIENCY,
                    Enchantment.SHARPNESS,
                    Enchantment.SILK_TOUCH,
                    Enchantment.UNBREAKING
            ),
            Material.DIAMOND_SHOVEL, Set.of(
                    Enchantment.EFFICIENCY,
                    Enchantment.SILK_TOUCH,
                    Enchantment.UNBREAKING
            ),
            Material.DIAMOND_HOE, Set.of(
                    Enchantment.EFFICIENCY,
                    Enchantment.SILK_TOUCH,
                    Enchantment.UNBREAKING
            )
    );
    private final TradesRebalance plugin;
    private final TradeManager tradeManager;

    public ToolsmithTradeListener(TradesRebalance plugin, TradeManager tradeManager) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onToolsmithTrade(VillagerAcquireTradeEvent event) {
        if (!plugin.getConfigManager().isToolsmithUpdateEnabled()) {
            return;
        }

        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }

        if (villager.getProfession() != Villager.Profession.TOOLSMITH) {
            return;
        }

        MerchantRecipe recipe = event.getRecipe();
        ItemStack result = recipe.getResult();

        // Enhance diamond tool trades
        if (isDiamondTool(result.getType()) && villager.getVillagerLevel() >= 4) {
            enhanceDiamondToolTrade(event, recipe, result, villager);
        }

        // Add netherite upgrade template trades for master toolsmiths
        if (villager.getVillagerLevel() >= 5) {
            addNetheriteUpgradeTrade(event, recipe);
        }
    }

    private void enhanceDiamondToolTrade(
            VillagerAcquireTradeEvent event,
            MerchantRecipe recipe,
            @NotNull ItemStack result,
            Villager villager) {

        Set<Enchantment> possibleEnchants = DIAMOND_TOOL_ENCHANTS.get(result.getType());
        if (possibleEnchants == null || possibleEnchants.isEmpty()) {
            return;
        }

        // 60% chance to add enchantments to diamond tools
        if (ThreadLocalRandom.current().nextDouble() > 0.6) {
            return;
        }

        ItemStack enhancedTool = result.clone();
        ItemMeta meta = enhancedTool.getItemMeta();

        if (meta == null) return;

        // Add 1-2 random enchantments
        int enchantCount = ThreadLocalRandom.current().nextInt(1, 3);
        List<Enchantment> enchantList = new ArrayList<>(possibleEnchants);
        Collections.shuffle(enchantList);

        for (int i = 0; i < Math.min(enchantCount, enchantList.size()); i++) {
            Enchantment enchant = enchantList.get(i);
            int level = calculateEnchantmentLevel(enchant);
            meta.addEnchant(enchant, level, true);
        }

        enhancedTool.setItemMeta(meta);

        // Adjust price based on enchantments
        var ingredients = new ArrayList<>(recipe.getIngredients());
        if (!ingredients.isEmpty()) {
            ItemStack emeralds = ingredients.getFirst();
            if (emeralds.getType() == Material.EMERALD) {
                int basePrice = emeralds.getAmount();
                int enchantBonus = meta.getEnchants().size() * 5;
                int newPrice = Math.clamp(basePrice + enchantBonus, 5, 64);
                emeralds.setAmount(newPrice);
                ingredients.set(0, emeralds);
            }
        }

        MerchantRecipe newRecipe = new MerchantRecipe(
                enhancedTool,
                recipe.getUses(),
                Math.max(recipe.getMaxUses(), 12),
                recipe.hasExperienceReward(),
                recipe.getVillagerExperience(),
                recipe.getPriceMultiplier(),
                recipe.getDemand(),
                recipe.getSpecialPrice()
        );

        newRecipe.setIngredients(ingredients);
        event.setRecipe(newRecipe);

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getSLF4JLogger().debug(
                    "Enhanced toolsmith trade: {} with {} enchantments",
                    result.getType(),
                    meta.getEnchants().size()
            );
        }
    }

    private int calculateEnchantmentLevel(@NotNull Enchantment enchant) {
        int maxLevel = enchant.getMaxLevel();
        if (maxLevel == 1) return 1;

        // 50% chance for max level, otherwise random
        return ThreadLocalRandom.current().nextDouble() < 0.5
                ? maxLevel
                : ThreadLocalRandom.current().nextInt(1, maxLevel) + 1;
    }

    private void addNetheriteUpgradeTrade(VillagerAcquireTradeEvent event, MerchantRecipe recipe) {
        // 25% chance to add netherite upgrade template
        if (ThreadLocalRandom.current().nextDouble() > 0.25) {
            return;
        }

        ItemStack template = new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        ItemStack cost = new ItemStack(Material.EMERALD, 16);
        ItemStack diamond = new ItemStack(Material.DIAMOND, 3);

        MerchantRecipe upgradeRecipe = new MerchantRecipe(
                template,
                0,
                2, // Very limited uses - netherite upgrades are valuable
                true,
                25,
                0.05f,
                0,
                0
        );

        upgradeRecipe.addIngredient(cost);
        upgradeRecipe.addIngredient(diamond);

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getSLF4JLogger().debug("Added netherite upgrade template trade");
        }
    }

    private boolean isDiamondTool(Material material) {
        return DIAMOND_TOOL_ENCHANTS.containsKey(material);
    }
}