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

public final class WeaponsmithTradeListener implements Listener {

    // Weapon enchantments by type
    private static final Map<Material, Set<Enchantment>> WEAPON_ENCHANTS = Map.of(
            Material.DIAMOND_SWORD, Set.of(
                    Enchantment.SHARPNESS,
                    Enchantment.SMITE,
                    Enchantment.BANE_OF_ARTHROPODS,
                    Enchantment.KNOCKBACK,
                    Enchantment.FIRE_ASPECT,
                    Enchantment.LOOTING,
                    Enchantment.SWEEPING_EDGE,
                    Enchantment.UNBREAKING
            ),
            Material.DIAMOND_AXE, Set.of(
                    Enchantment.SHARPNESS,
                    Enchantment.SMITE,
                    Enchantment.BANE_OF_ARTHROPODS,
                    Enchantment.EFFICIENCY,
                    Enchantment.UNBREAKING
            )
    );
    // Mace enchantments (1.21+)
    private static final Set<Enchantment> MACE_ENCHANTS = Set.of(
            Enchantment.DENSITY,
            Enchantment.BREACH,
            Enchantment.WIND_BURST,
            Enchantment.SMITE,
            Enchantment.BANE_OF_ARTHROPODS,
            Enchantment.FIRE_ASPECT,
            Enchantment.UNBREAKING,
            Enchantment.MENDING
    );
    private final TradesRebalance plugin;
    private final TradeManager tradeManager;

    public WeaponsmithTradeListener(TradesRebalance plugin, TradeManager tradeManager) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWeaponsmithTrade(VillagerAcquireTradeEvent event) {
        if (!plugin.getConfigManager().isWeaponsmithUpdateEnabled()) {
            return;
        }

        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }

        if (villager.getProfession() != Villager.Profession.WEAPONSMITH) {
            return;
        }

        MerchantRecipe recipe = event.getRecipe();
        ItemStack result = recipe.getResult();

        // Enhance diamond weapon trades
        if (isDiamondWeapon(result.getType()) && villager.getVillagerLevel() >= 4) {
            enhanceWeaponTrade(event, recipe, result, villager);
        }

        // Add mace trades for master weaponsmiths (1.21+ feature)
        if (villager.getVillagerLevel() >= 5) {
            addMaceTrade(event, recipe);
        }

        // Add bells for lower level weaponsmiths
        if (villager.getVillagerLevel() >= 3) {
            addBellTrade(event, recipe);
        }
    }

    private void enhanceWeaponTrade(
            VillagerAcquireTradeEvent event,
            MerchantRecipe recipe,
            @NotNull ItemStack result,
            Villager villager) {

        Set<Enchantment> possibleEnchants = WEAPON_ENCHANTS.get(result.getType());
        if (possibleEnchants == null || possibleEnchants.isEmpty()) {
            return;
        }

        // 70% chance to add enchantments to weapons
        if (ThreadLocalRandom.current().nextDouble() > 0.7) {
            return;
        }

        ItemStack enhancedWeapon = result.clone();
        ItemMeta meta = enhancedWeapon.getItemMeta();

        if (meta == null) return;

        // Add 1-3 random enchantments
        int enchantCount = ThreadLocalRandom.current().nextInt(1, 4);
        List<Enchantment> enchantList = new ArrayList<>(possibleEnchants);
        Collections.shuffle(enchantList);

        // Ensure no conflicting enchantments (e.g., Sharpness + Smite)
        Set<Enchantment> addedEnchants = new HashSet<>();
        for (Enchantment enchant : enchantList) {
            if (addedEnchants.size() >= enchantCount) break;

            if (canAddEnchantment(enchant, addedEnchants)) {
                int level = calculateEnchantmentLevel(enchant);
                meta.addEnchant(enchant, level, true);
                addedEnchants.add(enchant);
            }
        }

        enhancedWeapon.setItemMeta(meta);

        // Adjust price based on enchantments
        var ingredients = new ArrayList<>(recipe.getIngredients());
        if (!ingredients.isEmpty()) {
            ItemStack emeralds = ingredients.get(0);
            if (emeralds.getType() == Material.EMERALD) {
                int basePrice = emeralds.getAmount();
                int enchantBonus = calculateEnchantmentBonus(addedEnchants);
                int newPrice = Math.clamp(basePrice + enchantBonus, 5, 64);
                emeralds.setAmount(newPrice);
                ingredients.set(0, emeralds);
            }
        }

        MerchantRecipe newRecipe = new MerchantRecipe(
                enhancedWeapon,
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
                    "Enhanced weaponsmith trade: {} with enchantments: {}",
                    result.getType(),
                    addedEnchants
            );
        }
    }

    private boolean canAddEnchantment(Enchantment enchant, Set<Enchantment> existing) {
        // Check for conflicting enchantments
        Set<Enchantment> damageEnchants = Set.of(
                Enchantment.SHARPNESS,
                Enchantment.SMITE,
                Enchantment.BANE_OF_ARTHROPODS
        );

        if (damageEnchants.contains(enchant)) {
            for (Enchantment e : existing) {
                if (damageEnchants.contains(e) && e != enchant) {
                    return false;
                }
            }
        }

        return true;
    }

    private int calculateEnchantmentLevel(@NotNull Enchantment enchant) {
        int maxLevel = enchant.getMaxLevel();
        if (maxLevel == 1) return 1;

        // 60% chance for max level, otherwise random
        return ThreadLocalRandom.current().nextDouble() < 0.6
                ? maxLevel
                : ThreadLocalRandom.current().nextInt(1, maxLevel) + 1;
    }

    private int calculateEnchantmentBonus(@NotNull Set<Enchantment> enchants) {
        int bonus = 0;
        for (Enchantment e : enchants) {
            bonus += switch (e.getKey().getKey()) {
                case "sharpness", "looting", "fire_aspect" -> 8;
                case "smite", "bane_of_arthropods", "sweeping_edge" -> 5;
                case "knockback", "unbreaking" -> 3;
                default -> 2;
            };
        }
        return bonus;
    }

    private void addMaceTrade(VillagerAcquireTradeEvent event, MerchantRecipe recipe) {
        // 15% chance to offer mace
        if (ThreadLocalRandom.current().nextDouble() > 0.15) {
            return;
        }

        ItemStack mace = new ItemStack(Material.MACE);
        ItemMeta meta = mace.getItemMeta();

        if (meta != null) {
            // 50% chance to add 1-2 enchantments
            if (ThreadLocalRandom.current().nextDouble() < 0.5) {
                List<Enchantment> enchants = new ArrayList<>(MACE_ENCHANTS);
                Collections.shuffle(enchants);

                int count = ThreadLocalRandom.current().nextInt(1, 3);
                for (int i = 0; i < Math.min(count, enchants.size()); i++) {
                    Enchantment e = enchants.get(i);
                    int level = calculateEnchantmentLevel(e);
                    meta.addEnchant(e, level, true);
                }
            }

            mace.setItemMeta(meta);
        }

        ItemStack cost = new ItemStack(Material.EMERALD, 32);
        ItemStack breeze = new ItemStack(Material.BREEZE_ROD, 2);

        MerchantRecipe maceRecipe = new MerchantRecipe(
                mace,
                0,
                3, // Limited availability
                true,
                30,
                0.05f,
                0,
                0
        );

        maceRecipe.addIngredient(cost);
        maceRecipe.addIngredient(breeze);

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getSLF4JLogger().debug("Added mace trade with {} enchantments",
                    meta != null ? meta.getEnchants().size() : 0);
        }
    }

    private void addBellTrade(VillagerAcquireTradeEvent event, MerchantRecipe recipe) {
        // 20% chance to offer bell
        if (ThreadLocalRandom.current().nextDouble() > 0.2) {
            return;
        }

        ItemStack bell = new ItemStack(Material.BELL);
        ItemStack cost = new ItemStack(Material.EMERALD, 36);

        MerchantRecipe bellRecipe = new MerchantRecipe(
                bell,
                0,
                12,
                true,
                10,
                0.05f,
                0,
                0
        );

        bellRecipe.addIngredient(cost);

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getSLF4JLogger().debug("Added bell trade");
        }
    }

    private boolean isDiamondWeapon(Material material) {
        return WEAPON_ENCHANTS.containsKey(material);
    }
}