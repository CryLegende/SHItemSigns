package org.Cry_Legende.sHItemSigns;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /shitemsigns reload | clear [rayon] (alias /interactivesings et /ins, les noms d'InteractiveSigns). */
final class AdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("reload", "clear");
    private static final int MAX_RADIUS = 16;

    private final SHItemSigns plugin;
    private final SignItems items;

    AdminCommand(SHItemSigns plugin, SignItems items) {
        this.plugin = plugin;
        this.items = items;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        List<String> allowed = allowed(sender);
        if (allowed.isEmpty()) {
            return true;
        }
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        if (!allowed.contains(sub)) {
            plugin.messages().send(sender, "usage-admin", "commands", String.join("|", allowed));
            return true;
        }
        if (sub.equals("reload")) {
            reload(sender);
        } else {
            clear(sender, args);
        }
        return true;
    }

    private void reload(CommandSender sender) {
        try {
            plugin.reload();
            plugin.messages().send(sender, "recharge");
        } catch (SHItemSigns.ReloadException e) {
            plugin.getLogger().warning(e.file + " illisible, l'ancienne configuration reste active : " + e.getMessage());
            plugin.messages().send(sender, "rechargement-echec", "file", e.file, "error", e.getMessage());
        }
    }

    /** Reprend les objets des panneaux autour du joueur (cube de côté 2 × rayon) et les met dans son inventaire. */
    private void clear(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "joueurs-seulement");
            return;
        }
        int radius = 1;
        if (args.length >= 2) {
            try {
                radius = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                radius = -1;
            }
            if (radius < 1 || radius > MAX_RADIUS) {
                plugin.messages().send(player, "rayon-invalide", "radius", args[1]);
                return;
            }
        }
        int count = 0;
        for (ItemDisplay display : player.getWorld().getNearbyEntitiesByType(ItemDisplay.class, player.getLocation(), radius)) {
            if (display.isValid() && items.sideOf(display) != null) {
                items.give(player, display);
                count++;
            }
        }
        if (count == 0) {
            plugin.messages().send(player, "clear-aucun", "radius", String.valueOf(radius));
        } else {
            plugin.getLogger().info(player.getName() + " a repris " + count + " objet(s) de panneau autour de "
                    + player.getWorld().getName() + " " + player.getLocation().getBlockX() + " "
                    + player.getLocation().getBlockY() + " " + player.getLocation().getBlockZ() + " (rayon " + radius + ").");
            plugin.messages().send(player, "clear-fait", "count", String.valueOf(count), "radius", String.valueOf(radius));
        }
    }

    /** Sous-commandes que l'expéditeur peut utiliser. */
    private static List<String> allowed(CommandSender sender) {
        List<String> allowed = new ArrayList<>();
        for (String sub : SUBCOMMANDS) {
            if (sender.hasPermission("shitemsigns.admin." + sub)) {
                allowed.add(sub);
            }
        }
        return allowed;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
                                      @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], allowed(sender), completions);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("clear") && sender.hasPermission("shitemsigns.admin.clear")) {
            StringUtil.copyPartialMatches(args[1], List.of("1", "5", "16"), completions);
        }
        return completions;
    }
}
