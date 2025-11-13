package dev.oumaimaa.tradesrebalance.trades;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

public final class EnchantmentTradeFactory {

    private static final Set<Enchantment> TREASURE_ENCHANTMENTS = Set.of(
            Enchantment.MENDING,
            Enchantment.FROST_WALKER,
            Enchantment.BINDING_CURSE,
            Enchantment.VANISHING_CURSE,
            Enchantment.SOUL_SPEED,
            Enchantment.SWIFT_SNEAK
    );

    public Optional<MerchantRecipe> createEnchantmentTrade(
            Enchantment enchantment,
            int level,
            MerchantRecipe originalRecipe) {

        try {
            ItemStack enchantedBook = createEnchantedBook(enchantment, level);
            int price = calculatePrice(enchantment, level);

            var ingredients = new ArrayList<>(originalRecipe.getIngredients());
            if (!ingredients.isEmpty() && ingredients.getFirst().getType() == Material.EMERALD) {
                ingredients.getFirst().setAmount(Math.clamp(price, 1, 64));
            } else {
                ingredients.clear();
                ingredients.add(new ItemStack(Material.EMERALD, Math.clamp(price, 1, 64)));
            }

            // Add book requirement for expensive enchantments
            if (price > 20 && ingredients.size() < 2) {
                ingredients.add(new ItemStack(Material.BOOK, 1));
            }

            MerchantRecipe newRecipe = new MerchantRecipe(
                    enchantedBook,
                    originalRecipe.getUses(),
                    Math.max(originalRecipe.getMaxUses(), 12),
                    originalRecipe.hasExperienceReward(),
                    originalRecipe.getVillagerExperience(),
                    originalRecipe.getPriceMultiplier(),
                    originalRecipe.getDemand(),
                    originalRecipe.getSpecialPrice()
            );

            newRecipe.setIngredients(ingredients);
            return Optional.of(newRecipe);

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private @NotNull ItemStack createEnchantedBook(Enchantment enchantment, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);

        if (book.getItemMeta() instanceof EnchantmentStorageMeta meta) {
            meta.addStoredEnchant(enchantment, level, true);
            book.setItemMeta(meta);
        }

        return book;
    }

    private int calculatePrice(Enchantment enchantment, int level) {
        int basePrice = getBasePrice(enchantment);
        int levelMultiplier = (level - 1) * 5;
        int rarityBonus = getRarityBonus(enchantment);

        return basePrice + levelMultiplier + rarityBonus;
    }

    private int getBasePrice(Enchantment enchantment) {
        if (TREASURE_ENCHANTMENTS.contains(enchantment)) {
            return 25;
        }

        return switch (enchantment.getMaxLevel()) {
            case 1 -> 15;
            case 2 -> 12;
            case 3 -> 10;
            case 4 -> 8;
            default -> 5;
        };
    }

    private int getRarityBonus(Enchantment enchantment) {
        // Special high-value enchantments
        if (enchantment == Enchantment.MENDING) return 15;
        if (enchantment == Enchantment.SILK_TOUCH) return 10;
        if (enchantment == Enchantment.FORTUNE) return 8;
        if (enchantment == Enchantment.LOOTING) return 8;

        return 0;
    }

    public boolean isValidEnchantmentBook(ItemStack item) {
        if (item == null || item.getType() != Material.ENCHANTED_BOOK) {
            return false;
        }

        return item.getItemMeta() instanceof EnchantmentStorageMeta meta
                && meta.hasStoredEnchants();
    }

    public Optional<Enchantment> getStoredEnchantment(ItemStack book) {
        if (!isValidEnchantmentBook(book)) {
            return Optional.empty();
        }

        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        return meta.getStoredEnchants().keySet().stream().findFirst();
    }
}