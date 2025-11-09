package dev.oumaimaa.tradesrebalance.trades;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class WanderingTraderTradeFactory {

    private static final List<Material> ALL_DYES = Stream.of(
            Material.RED_DYE,
            Material.WHITE_DYE,
            Material.BLUE_DYE,
            Material.PINK_DYE,
            Material.BLACK_DYE,
            Material.GREEN_DYE,
            Material.ORANGE_DYE,
            Material.MAGENTA_DYE,
            Material.LIGHT_BLUE_DYE,
            Material.YELLOW_DYE,
            Material.LIME_DYE,
            Material.CYAN_DYE,
            Material.PURPLE_DYE,
            Material.BROWN_DYE,
            Material.LIGHT_GRAY_DYE,
            Material.GRAY_DYE
    ).toList();

    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    public List<MerchantRecipe> createDyeTrades(int count) {
        if (count <= 0 || count > ALL_DYES.size()) {
            return Collections.emptyList();
        }

        // Select random unique dyes
        List<Material> shuffled = new ArrayList<>(ALL_DYES);
        Collections.shuffle(shuffled, new Random(random.nextLong()));

        return shuffled.stream()
                .limit(count)
                .map(this::createDyeTrade)
                .collect(Collectors.toList());
    }

    private @NotNull MerchantRecipe createDyeTrade(Material dyeType) {
        ItemStack result = new ItemStack(dyeType, 3);
        ItemStack cost = new ItemStack(Material.EMERALD, 1);

        MerchantRecipe recipe = new MerchantRecipe(
                result,
                0,        // uses
                12,       // max uses
                true,     // experience reward
                2,        // villager experience
                0.05f,    // price multiplier
                0,        // demand
                0         // special price
        );

        recipe.addIngredient(cost);
        return recipe;
    }

    public @NotNull Optional<MerchantRecipe> adjustPrice(MerchantRecipe recipe, int adjustment) {
        try {
            var ingredients = new ArrayList<>(recipe.getIngredients());

            if (ingredients.isEmpty()) {
                return Optional.of(recipe);
            }

            ItemStack firstIngredient = ingredients.getFirst();
            if (firstIngredient.getType() != Material.EMERALD) {
                return Optional.of(recipe);
            }

            int currentAmount = firstIngredient.getAmount();
            int newAmount = Math.clamp(currentAmount + adjustment, 1, 64);

            if (newAmount == currentAmount) {
                return Optional.of(recipe);
            }

            firstIngredient.setAmount(newAmount);
            ingredients.set(0, firstIngredient);

            MerchantRecipe adjustedRecipe = new MerchantRecipe(
                    recipe.getResult(),
                    recipe.getUses(),
                    recipe.getMaxUses(),
                    recipe.hasExperienceReward(),
                    recipe.getVillagerExperience(),
                    recipe.getPriceMultiplier(),
                    recipe.getDemand(),
                    recipe.getSpecialPrice()
            );

            adjustedRecipe.setIngredients(ingredients);
            return Optional.of(adjustedRecipe);

        } catch (Exception e) {
            return Optional.of(recipe);
        }
    }

    public List<MerchantRecipe> createBundleTrade(Material @NotNull ... items) {
        if (items.length == 0) {
            return Collections.emptyList();
        }

        return Arrays.stream(items)
                .map(this::createSingleItemTrade)
                .collect(Collectors.toList());
    }

    private @NotNull MerchantRecipe createSingleItemTrade(Material material) {
        ItemStack result = new ItemStack(material, calculateBundleSize(material));
        ItemStack cost = new ItemStack(Material.EMERALD, calculatePrice(material));

        MerchantRecipe recipe = new MerchantRecipe(
                result,
                0,
                8,
                true,
                1,
                0.05f,
                0,
                0
        );

        recipe.addIngredient(cost);
        return recipe;
    }

    private int calculateBundleSize(@NotNull Material material) {
        return switch (material.getMaxStackSize()) {
            case 1 -> 1;
            case 16 -> random.nextInt(1, 4);
            case 64 -> random.nextInt(2, 8);
            default -> random.nextInt(1, 5);
        };
    }

    private int calculatePrice(@NotNull Material material) {
        // Base price calculation for wandering trader items
        if (material.isBlock()) {
            return random.nextInt(1, 4);
        }

        return random.nextInt(1, 3);
    }
}