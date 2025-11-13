package dev.oumaimaa.tradesrebalance.listeners;

import dev.oumaimaa.tradesrebalance.TradesRebalance;
import dev.oumaimaa.tradesrebalance.managers.TradeManager;
import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class ArmorerTradeListener implements Listener {

    private static final Map<Integer, Set<Material>> LEVEL_ARMOR = Map.of(
            3, Set.of(Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS),
            4, Set.of(Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE),
            5, Set.of(Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE,
                    Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS)
    );

    private static final Set<Material> CHAINMAIL_ARMOR = Set.of(
            Material.CHAINMAIL_HELMET,
            Material.CHAINMAIL_CHESTPLATE,
            Material.CHAINMAIL_LEGGINGS,
            Material.CHAINMAIL_BOOTS
    );
    private final TradesRebalance plugin;
    private final TradeManager tradeManager;

    public ArmorerTradeListener(TradesRebalance plugin, TradeManager tradeManager) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArmorerTrade(VillagerAcquireTradeEvent event) {
        if (!plugin.getConfigManager().isArmorerUpdateEnabled()) {
            return;
        }

        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }

        if (villager.getProfession() != Villager.Profession.ARMORER) {
            return;
        }

        MerchantRecipe recipe = event.getRecipe();
        ItemStack result = recipe.getResult();

        if (isDiamondArmor(result.getType())) {
            modifyDiamondArmorTrade(event, recipe, result, villager);
        }

        if (CHAINMAIL_ARMOR.contains(result.getType())) {
            modifyChainmailTrade(event, recipe, result);
        }

        if (villager.getVillagerLevel() >= 5) {
            addArmorTrimTrade(event, recipe);
        }
    }

    private void modifyDiamondArmorTrade(
            VillagerAcquireTradeEvent event,
            MerchantRecipe recipe,
            @NotNull ItemStack result,
            @NotNull Villager villager) {

        int level = villager.getVillagerLevel();
        Set<Material> allowedArmor = LEVEL_ARMOR.getOrDefault(level, Collections.emptySet());

        // Check if this armor piece is allowed at this level
        if (!allowedArmor.contains(result.getType())) {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getSLF4JLogger().debug(
                        "Blocked diamond armor trade {} at level {}",
                        result.getType(),
                        level
                );
            }
            return;
        }

        // Adjust prices based on armor piece
        var ingredients = new ArrayList<>(recipe.getIngredients());
        if (ingredients.isEmpty()) return;

        ItemStack emeralds = ingredients.get(0);
        if (emeralds.getType() != Material.EMERALD) return;

        int newPrice = calculateDiamondArmorPrice(result.getType(), level);
        emeralds.setAmount(Math.clamp(newPrice, 5, 64));
        ingredients.set(0, emeralds);

        MerchantRecipe newRecipe = new MerchantRecipe(
                result,
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
                    "Modified diamond armor trade: {} at level {} for {} emeralds",
                    result.getType(),
                    level,
                    newPrice
            );
        }
    }

    @Contract(pure = true)
    private int calculateDiamondArmorPrice(@NotNull Material armor, int level) {
        int basePrice = switch (armor) {
            case DIAMOND_HELMET, DIAMOND_BOOTS -> 13;
            case DIAMOND_CHESTPLATE -> 21;
            case DIAMOND_LEGGINGS -> 19;
            default -> 15;
        };

        // Reduce price slightly for higher level villagers
        int levelDiscount = (level - 3) * 2;
        return Math.max(5, basePrice - levelDiscount);
    }

    private void modifyChainmailTrade(
            VillagerAcquireTradeEvent event,
            @NotNull MerchantRecipe recipe,
            ItemStack result) {

        var ingredients = new ArrayList<>(recipe.getIngredients());
        if (ingredients.isEmpty()) return;

        ItemStack emeralds = ingredients.get(0);
        if (emeralds.getType() != Material.EMERALD) return;

        // Make chainmail cheaper (was very expensive before)
        int newPrice = calculateChainmailPrice(result.getType());
        emeralds.setAmount(Math.clamp(newPrice, 1, 15));
        ingredients.set(0, emeralds);

        MerchantRecipe newRecipe = new MerchantRecipe(
                result,
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
                    "Modified chainmail trade: {} for {} emeralds",
                    result.getType(),
                    newPrice
            );
        }
    }

    @Contract(pure = true)
    private int calculateChainmailPrice(@NotNull Material armor) {
        return switch (armor) {
            case CHAINMAIL_HELMET, CHAINMAIL_BOOTS -> 4;
            case CHAINMAIL_LEGGINGS -> 6;
            case CHAINMAIL_CHESTPLATE -> 8;
            default -> 5;
        };
    }

    private void addArmorTrimTrade(VillagerAcquireTradeEvent event, MerchantRecipe recipe) {
        // 40% chance to add armor trim template trade for master armorers
        if (ThreadLocalRandom.current().nextDouble() > 0.4) {
            return;
        }

        // Armor trim templates available from armorers
        Material[] trimTemplates = {
                Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE,
                Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE,
                Material.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE,
                Material.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE,
                Material.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE,
                Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE
        };

        Material selectedTemplate = trimTemplates[
                ThreadLocalRandom.current().nextInt(trimTemplates.length)
                ];

        ItemStack template = new ItemStack(selectedTemplate);
        ItemStack cost = new ItemStack(Material.EMERALD, 12);
        ItemStack diamond = new ItemStack(Material.DIAMOND, 2);

        MerchantRecipe trimRecipe = new MerchantRecipe(
                template,
                0,
                3, // Limited uses - trim templates are rare
                true,
                20,
                0.05f,
                0,
                0
        );

        trimRecipe.addIngredient(cost);
        trimRecipe.addIngredient(diamond);

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getSLF4JLogger().debug(
                    "Added armor trim template trade: {}",
                    selectedTemplate
            );
        }
    }

    @Contract(pure = true)
    private boolean isDiamondArmor(@NotNull Material material) {
        return switch (material) {
            case DIAMOND_HELMET, DIAMOND_CHESTPLATE,
                 DIAMOND_LEGGINGS, DIAMOND_BOOTS -> true;
            default -> false;
        };
    }
}