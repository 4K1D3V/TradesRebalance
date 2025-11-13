package dev.oumaimaa.tradesrebalance.config;

import dev.oumaimaa.tradesrebalance.TradesRebalance;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public final class ConfigurationManager {

    private final TradesRebalance plugin;
    private final Path dataFolder;
    private final Map<String, Set<Enchantment>> biomeEnchantments = new EnumMap<>(BiomeType.class)
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                    e -> e.getKey().name(),
                    e -> new HashSet<>()
            ));
    private FileConfiguration config;
    private boolean librarianNerfEnabled;
    private boolean wanderingTraderUpdateEnabled;
    private boolean cartographerUpdateEnabled;
    private boolean armorerUpdateEnabled;
    private boolean toolsmithUpdateEnabled;
    private boolean weaponsmithUpdateEnabled;
    private boolean lootTableUpdateEnabled;
    private int newTradesCount;
    private boolean adjustCommonPrices;
    private boolean adjustRarePrices;
    private boolean debugMode;

    public ConfigurationManager(@NotNull TradesRebalance plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder().toPath();

        initializeDataFolder();
        loadConfiguration();
    }

    private void initializeDataFolder() {
        try {
            if (Files.notExists(dataFolder)) {
                Files.createDirectories(dataFolder);
            }
        } catch (IOException e) {
            plugin.getSLF4JLogger().error("Failed to create data folder", e);
        }
    }

    private void loadConfiguration() {
        File configFile = dataFolder.resolve("config.yml").toFile();

        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        loadValues();
    }

    private void loadValues() {

        librarianNerfEnabled = config.getBoolean("enable-librarian-nerf", true);
        wanderingTraderUpdateEnabled = config.getBoolean("enable-wandering-trader-update", true);
        cartographerUpdateEnabled = config.getBoolean("enable-cartographer-update", true);
        armorerUpdateEnabled = config.getBoolean("enable-armorer-update", true);
        toolsmithUpdateEnabled = config.getBoolean("enable-toolsmith-update", true);
        weaponsmithUpdateEnabled = config.getBoolean("enable-weaponsmith-update", true);
        lootTableUpdateEnabled = config.getBoolean("enable-loot-table-update", true);
        debugMode = config.getBoolean("debug", false);

        newTradesCount = config.getInt("wandering-trader.new-trades-count", 2);
        adjustCommonPrices = config.getBoolean("wandering-trader.adjust-common-prices", true);
        adjustRarePrices = config.getBoolean("wandering-trader.adjust-rare-prices", true);

        loadBiomeEnchantments();

        plugin.getSLF4JLogger().info("Configuration loaded successfully");
    }

    private void loadBiomeEnchantments() {
        ConfigurationSection section = config.getConfigurationSection("librarian.biome-enchantments");

        if (section == null) {
            loadDefaultBiomeEnchantments();
            return;
        }

        section.getKeys(false).forEach(biome -> {
            List<String> enchantNames = section.getStringList(biome);
            Set<Enchantment> enchantments = enchantNames.stream()
                    .map(this::parseEnchantment)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());

            biomeEnchantments.put(biome.toUpperCase(), enchantments);
        });
    }

    private Optional<Enchantment> parseEnchantment(String name) {
        try {
            return Optional.ofNullable(Enchantment.getByKey(NamespacedKey.minecraft(name.toLowerCase())));
        } catch (Exception e) {
            plugin.getSLF4JLogger().warn("Failed to parse enchantment: {}", name);
            return Optional.empty();
        }
    }

    private void loadDefaultBiomeEnchantments() {
        biomeEnchantments.put("DESERT", Set.of(
                Enchantment.FIRE_PROTECTION,
                Enchantment.THORNS,
                Enchantment.INFINITY
        ));

        biomeEnchantments.put("JUNGLE", Set.of(
                Enchantment.FEATHER_FALLING,
                Enchantment.PROJECTILE_PROTECTION,
                Enchantment.UNBREAKING
        ));

        biomeEnchantments.put("PLAINS", Set.of(
                Enchantment.PROTECTION,
                Enchantment.PUNCH,
                Enchantment.SMITE
        ));

        biomeEnchantments.put("SAVANNA", Set.of(
                Enchantment.SHARPNESS,
                Enchantment.KNOCKBACK,
                Enchantment.BINDING_CURSE
        ));

        biomeEnchantments.put("SNOWY", Set.of(
                Enchantment.AQUA_AFFINITY,
                Enchantment.LOOTING,
                Enchantment.FROST_WALKER
        ));

        biomeEnchantments.put("SWAMP", Set.of(
                Enchantment.DEPTH_STRIDER,
                Enchantment.RESPIRATION,
                Enchantment.MENDING
        ));

        biomeEnchantments.put("TAIGA", Set.of(
                Enchantment.BLAST_PROTECTION,
                Enchantment.FIRE_ASPECT,
                Enchantment.FLAME
        ));
    }

    public void reload() {
        loadConfiguration();
    }

    public boolean isLibrarianNerfEnabled() {
        return librarianNerfEnabled;
    }

    public boolean isWanderingTraderUpdateEnabled() {
        return wanderingTraderUpdateEnabled;
    }

    public boolean isCartographerUpdateEnabled() {
        return cartographerUpdateEnabled;
    }

    public boolean isArmorerUpdateEnabled() {
        return armorerUpdateEnabled;
    }

    public boolean isToolsmithUpdateEnabled() {
        return toolsmithUpdateEnabled;
    }

    public boolean isWeaponsmithUpdateEnabled() {
        return weaponsmithUpdateEnabled;
    }

    public boolean isLootTableUpdateEnabled() {
        return lootTableUpdateEnabled;
    }

    public int getNewTradesCount() {
        return newTradesCount;
    }

    public boolean shouldAdjustCommonPrices() {
        return adjustCommonPrices;
    }

    public boolean shouldAdjustRarePrices() {
        return adjustRarePrices;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public Set<Enchantment> getBiomeEnchantments(@NotNull String biomeType) {
        return biomeEnchantments.getOrDefault(biomeType.toUpperCase(),
                getAllEnchantments());
    }

    private Set<Enchantment> getAllEnchantments() {
        return Arrays.stream(Enchantment.values())
                .collect(Collectors.toSet());
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public enum BiomeType {
        DESERT, JUNGLE, PLAINS, SAVANNA, SNOWY, SWAMP, TAIGA
    }
}