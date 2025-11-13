package dev.oumaimaa.tradesrebalance.commands;

import dev.oumaimaa.tradesrebalance.TradesRebalance;
import dev.oumaimaa.tradesrebalance.config.ConfigurationManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

public final class TradesRebalanceCommand implements TabExecutor {

    private static final String PERMISSION_BASE = "tradesrebalance.";
    private static final String PERMISSION_RELOAD = PERMISSION_BASE + "reload";
    private final TradesRebalance plugin;
    private final ConfigurationManager configManager;

    public TradesRebalanceCommand(TradesRebalance plugin, ConfigurationManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String @NotNull [] args) {

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "info" -> handleInfo(sender);
            case "help" -> {
                sendHelp(sender);
                yield true;
            }
            case "debug" -> handleDebug(sender, args);
            default -> {
                sender.sendMessage(
                        Component.text("Unknown subcommand. Use ", NamedTextColor.RED)
                                .append(Component.text("/tradesrebalance help", NamedTextColor.YELLOW))
                );
                yield true;
            }
        };
    }

    private boolean handleReload(@NotNull CommandSender sender) {
        if (!sender.hasPermission(PERMISSION_RELOAD)) {
            sender.sendMessage(
                    Component.text("You don't have permission to use this command!", NamedTextColor.RED)
            );
            return true;
        }

        try {
            plugin.reloadConfiguration();
            sender.sendMessage(
                    Component.text("✓ ", NamedTextColor.GREEN)
                            .append(Component.text("Configuration reloaded successfully!", NamedTextColor.WHITE))
            );
        } catch (Exception e) {
            sender.sendMessage(
                    Component.text("✗ ", NamedTextColor.RED)
                            .append(Component.text("Failed to reload configuration: ", NamedTextColor.WHITE))
                            .append(Component.text(e.getMessage(), NamedTextColor.GRAY))
            );
            plugin.getSLF4JLogger().error("Failed to reload configuration", e);
        }

        return true;
    }

    private boolean handleInfo(@NotNull CommandSender sender) {
        var meta = plugin.getPluginMeta();

        sender.sendMessage(
                Component.text()
                        .append(Component.text("═══ ", NamedTextColor.GOLD, TextDecoration.BOLD))
                        .append(Component.text("TradesRebalance", NamedTextColor.YELLOW, TextDecoration.BOLD))
                        .append(Component.text(" ═══", NamedTextColor.GOLD, TextDecoration.BOLD))
                        .build()
        );

        sender.sendMessage(Component.empty());

        sender.sendMessage(
                Component.text("Version: ", NamedTextColor.YELLOW)
                        .append(Component.text(meta.getVersion(), NamedTextColor.WHITE))
        );

        sender.sendMessage(
                Component.text("Replicates: ", NamedTextColor.YELLOW)
                        .append(Component.text("Minecraft 23w31a trade changes", NamedTextColor.WHITE))
        );

        sender.sendMessage(Component.empty());

        sender.sendMessage(
                Component.text("Features:", NamedTextColor.YELLOW, TextDecoration.BOLD)
        );

        sender.sendMessage(
                Component.text("  • ", NamedTextColor.GRAY)
                        .append(Component.text("Librarian biome-specific enchanted book trades", NamedTextColor.WHITE))
        );

        sender.sendMessage(
                Component.text("  • ", NamedTextColor.GRAY)
                        .append(Component.text("Wandering trader updated offers with dyes", NamedTextColor.WHITE))
        );

        sender.sendMessage(
                Component.text("  • ", NamedTextColor.GRAY)
                        .append(Component.text("Configurable trade modifications", NamedTextColor.WHITE))
        );

        sender.sendMessage(Component.empty());

        sendFeatureStatus(sender, "Librarian Nerf", configManager.isLibrarianNerfEnabled());
        sendFeatureStatus(sender, "Wandering Trader Update", configManager.isWanderingTraderUpdateEnabled());
        sendFeatureStatus(sender, "Debug Mode", configManager.isDebugMode());

        return true;
    }

    private void sendFeatureStatus(@NotNull CommandSender sender, String feature, boolean enabled) {
        sender.sendMessage(
                Component.text(feature + ": ", NamedTextColor.YELLOW)
                        .append(Component.text(
                                enabled ? "✓ Enabled" : "✗ Disabled",
                                enabled ? NamedTextColor.GREEN : NamedTextColor.RED
                        ))
        );
    }

    private boolean handleDebug(@NotNull CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION_RELOAD)) {
            sender.sendMessage(
                    Component.text("You don't have permission to use this command!", NamedTextColor.RED)
            );
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(
                    Component.text("Usage: /tradesrebalance debug <on|off>", NamedTextColor.RED)
            );
            return true;
        }

        boolean enable = switch (args[1].toLowerCase()) {
            case "on", "true", "enable" -> true;
            case "off", "false", "disable" -> false;
            default -> {
                sender.sendMessage(
                        Component.text("Invalid option. Use: on, off, true, false, enable, or disable", NamedTextColor.RED)
                );
                yield configManager.isDebugMode();
            }
        };

        configManager.getConfig().set("debug", enable);
        try {
            configManager.getConfig().save(plugin.getDataFolder().toPath().resolve("config.yml").toFile());
            plugin.reloadConfiguration();

            sender.sendMessage(
                    Component.text("Debug mode ", NamedTextColor.WHITE)
                            .append(Component.text(
                                    enable ? "enabled" : "disabled",
                                    enable ? NamedTextColor.GREEN : NamedTextColor.RED
                            ))
            );
        } catch (Exception e) {
            sender.sendMessage(
                    Component.text("Failed to save configuration", NamedTextColor.RED)
            );
        }

        return true;
    }

    private void sendHelp(@NotNull CommandSender sender) {
        sender.sendMessage(
                Component.text()
                        .append(Component.text("═══ ", NamedTextColor.GOLD, TextDecoration.BOLD))
                        .append(Component.text("TradesRebalance Help", NamedTextColor.YELLOW, TextDecoration.BOLD))
                        .append(Component.text(" ═══", NamedTextColor.GOLD, TextDecoration.BOLD))
                        .build()
        );

        sender.sendMessage(Component.empty());

        sendCommandHelp(sender, "/tradesrebalance reload", "Reload configuration");
        sendCommandHelp(sender, "/tradesrebalance info", "Show plugin information");
        sendCommandHelp(sender, "/tradesrebalance debug <on|off>", "Toggle debug mode");
        sendCommandHelp(sender, "/tradesrebalance help", "Show this help message");
    }

    private void sendCommandHelp(@NotNull CommandSender sender, String command, String description) {
        sender.sendMessage(
                Component.text("  " + command, NamedTextColor.YELLOW)
                        .append(Component.text(" - ", NamedTextColor.GRAY))
                        .append(Component.text(description, NamedTextColor.WHITE))
        );
    }

    @Override
    public @NotNull List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String @NotNull [] args) {

        if (args.length == 1) {
            return Stream.of("reload", "info", "help", "debug")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            return Stream.of("on", "off", "enable", "disable")
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .toList();
        }

        return List.of();
    }
}