package dev.oumaimaa.tradesrebalance.listeners;

import dev.oumaimaa.tradesrebalance.TradesRebalance;
import dev.oumaimaa.tradesrebalance.managers.LootTableManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTables;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class LootTableListener implements Listener {

    private final TradesRebalance plugin;
    private final LootTableManager lootManager;

    public LootTableListener(TradesRebalance plugin, LootTableManager lootManager) {
        this.plugin = plugin;
        this.lootManager = lootManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLootGenerate(LootGenerateEvent event) {
        if (!plugin.getConfigManager().isLootTableUpdateEnabled()) {
            return;
        }

        Optional<LootTables> tableType = getLootTableType(event);
        if (tableType.isEmpty()) {
            return;
        }

        if (!lootManager.isLootTableEnhanced(tableType.get())) {
            return;
        }

        List<ItemStack> originalLoot = new ArrayList<>(event.getLoot());

        List<ItemStack> enhancedLoot = lootManager.enhanceLoot(
                tableType.get(),
                originalLoot,
                event.getLootContext()
        );

        event.setLoot(enhancedLoot);

        if (plugin.getConfigManager().isDebugMode()) {
            Optional<String> playerName = Optional.ofNullable(event.getEntity())
                    .filter(entity -> entity instanceof Player)
                    .map(CommandSender::getName);

            plugin.getSLF4JLogger().debug(
                    "Enhanced loot generation: {} (player: {}, location: {}, items: {} -> {})",
                    tableType.get(),
                    playerName.orElse("none"),
                    event.getLootContext().getLocation(),
                    originalLoot.size(),
                    enhancedLoot.size()
            );
        }
    }

    private Optional<LootTables> getLootTableType(@NotNull LootGenerateEvent event) {
        if (event.getLootTable() == null) {
            return Optional.empty();
        }

        String key = event.getLootTable().getKey().toString();

        try {
            // Extract the table name from the key (e.g., "minecraft:chests/ancient_city" -> "ANCIENT_CITY")
            String tableName = extractTableName(key);

            for (LootTables table : LootTables.values()) {
                if (table.getKey().getKey().equalsIgnoreCase(tableName) ||
                        table.name().equalsIgnoreCase(tableName)) {
                    return Optional.of(table);
                }
            }
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getSLF4JLogger().debug(
                        "Could not parse loot table: {}",
                        key,
                        e
                );
            }
        }

        return Optional.empty();
    }

    private @NotNull String extractTableName(@NotNull String key) {
        // Extract table name from keys like:
        // "minecraft:chests/ancient_city" -> "ancient_city"
        // "minecraft:entities/zombie" -> "zombie"

        if (key.contains("/")) {
            String[] parts = key.split("/");
            return parts[parts.length - 1].toUpperCase();
        }

        if (key.contains(":")) {
            String[] parts = key.split(":");
            return parts[parts.length - 1].toUpperCase();
        }

        return key.toUpperCase();
    }
}