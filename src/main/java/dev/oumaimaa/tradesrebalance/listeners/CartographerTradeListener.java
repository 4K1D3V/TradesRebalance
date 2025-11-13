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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class CartographerTradeListener implements Listener {

    // Structure maps that shouldn't be sold
    private static final Set<Material> REMOVED_MAPS = Set.of(
            Material.FILLED_MAP // Will check NBT for structure maps
    );
    // New explorer maps with better prices
    private static final Set<String> ALLOWED_STRUCTURES = Set.of(
            "trial_chambers",
            "village",
            "mansion",
            "monument",
            "buried_treasure",
            "fortress",
            "stronghold",
            "end_city"
    );
    private final TradesRebalance plugin;
    private final TradeManager tradeManager;

    public CartographerTradeListener(TradesRebalance plugin, TradeManager tradeManager) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCartographerTrade(VillagerAcquireTradeEvent event) {
        if (!plugin.getConfigManager().isCartographerUpdateEnabled()) {
            return;
        }

        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }

        if (villager.getProfession() != Villager.Profession.CARTOGRAPHER) {
            return;
        }

        MerchantRecipe recipe = event.getRecipe();
        ItemStack result = recipe.getResult();

        // Check if it's a map trade
        if (result.getType() == Material.FILLED_MAP) {
            modifyMapTrade(event, recipe, result, villager);
        }

        // Add new banner pattern trades
        if (villager.getVillagerLevel() >= 4) {
            addBannerPatternTrade(event, recipe);
        }
    }

    private void modifyMapTrade(
            VillagerAcquireTradeEvent event,
            MerchantRecipe recipe,
            @NotNull ItemStack result,
            Villager villager) {

        var meta = result.getItemMeta();
        if (meta == null) return;

        // Adjust map prices based on distance and rarity
        var ingredients = new ArrayList<>(recipe.getIngredients());
        if (ingredients.isEmpty()) return;

        ItemStack emeralds = ingredients.get(0);
        if (emeralds.getType() != Material.EMERALD) return;

        // Reduce price for common maps, keep high for rare structures
        int currentPrice = emeralds.getAmount();
        int adjustedPrice = calculateMapPrice(currentPrice, villager.getVillagerLevel());

        emeralds.setAmount(Math.clamp(adjustedPrice, 7, 28));
        ingredients.set(0, emeralds);

        // Require compass for all map trades
        if (ingredients.size() < 2) {
            ingredients.add(new ItemStack(Material.COMPASS, 1));
        }

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
                    "Modified cartographer map trade: price {} -> {}",
                    currentPrice,
                    adjustedPrice
            );
        }
    }

    private int calculateMapPrice(int basePrice, int villagerLevel) {
        // Apprentice (2): cheaper maps
        // Journeyman (3): moderate prices
        // Expert (4): expensive rare maps
        // Master (5): very expensive special maps

        return switch (villagerLevel) {
            case 2 -> Math.max(7, basePrice - 5);
            case 3 -> Math.max(10, basePrice - 3);
            case 4 -> Math.min(20, basePrice);
            case 5 -> Math.min(28, basePrice + 2);
            default -> basePrice;
        };
    }

    private void addBannerPatternTrade(VillagerAcquireTradeEvent event, MerchantRecipe recipe) {
        // Chance to add rare banner pattern trades for expert cartographers
        if (ThreadLocalRandom.current().nextDouble() > 0.3) {
            return;
        }

        Material[] bannerPatterns = {
                Material.GLOBE_BANNER_PATTERN,
                Material.PIGLIN_BANNER_PATTERN
        };

        Material selectedPattern = bannerPatterns[
                ThreadLocalRandom.current().nextInt(bannerPatterns.length)
                ];

        ItemStack pattern = new ItemStack(selectedPattern);
        ItemStack cost = new ItemStack(Material.EMERALD, 8);

        MerchantRecipe patternRecipe = new MerchantRecipe(
                pattern,
                0,
                12,
                true,
                15,
                0.05f,
                0,
                0
        );

        patternRecipe.addIngredient(cost);

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getSLF4JLogger().debug(
                    "Added banner pattern trade: {}",
                    selectedPattern
            );
        }
    }
}