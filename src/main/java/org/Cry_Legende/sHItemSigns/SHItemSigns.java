package org.Cry_Legende.sHItemSigns;

import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;

/**
 * SHItemSigns : objets posés sur les panneaux (remplace InteractiveSigns). Les objets sont des item_display vanilla,
 * sauvegardés avec leur chunk ; ceux d'InteractiveSigns sont repris au chargement de leur chunk. Un objet dont le
 * panneau n'existe plus est supprimé. Les protections
 * d'île (SuperiorSkyblock2) et de région (WorldGuard) sont facultatives.
 */
public final class SHItemSigns extends JavaPlugin {

    private volatile Settings settings;
    private volatile Messages messages;
    private SignItems items;
    private BukkitTask sweepTask;

    @Override
    public void onEnable() {
        // Les deux plugins ensemble poseraient et lâcheraient chaque objet deux fois : SHItemSigns s'efface
        if (getServer().getPluginManager().getPlugin("InteractiveSigns") != null) {
            getLogger().severe("InteractiveSigns est encore installé : SHItemSigns ne démarre pas. Retirer "
                    + "InteractiveSigns puis redémarrer (ses objets seront repris).");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        saveDefaultConfig();
        if (!new File(getDataFolder(), "messages.yml").exists()) {
            saveResource("messages.yml", false);
        }
        settings = Settings.load(getConfig());
        try {
            messages = Messages.load(this);
        } catch (IOException | InvalidConfigurationException e) {
            getLogger().warning("messages.yml illisible, textes du jar utilisés : " + e.getMessage());
            messages = Messages.defaults(this);
        }

        RegionGuard regions = null;
        if (getServer().getPluginManager().isPluginEnabled("WorldGuard")) {
            try {
                regions = new RegionGuard();
            } catch (LinkageError e) {
                getLogger().warning("WorldGuard illisible, protection des régions désactivée : " + e);
            }
        }
        IslandGuard islands = null;
        if (getServer().getPluginManager().isPluginEnabled("SuperiorSkyblock2")) {
            try {
                islands = new IslandGuard();
            } catch (LinkageError e) {
                getLogger().warning("SuperiorSkyblock2 illisible, protection des îles désactivée : " + e);
            }
        }

        SignItems items = new SignItems(this, islands, regions);
        getServer().getPluginManager().registerEvents(new SignListener(this, items), this);
        PluginCommand command = getCommand("shitemsigns");
        if (command != null) {
            AdminCommand executor = new AdminCommand(this, items);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        // Chunks déjà chargés avant l'activation : leurs objets n'auront pas d'événement de chargement
        int adopted = 0;
        for (World world : getServer().getWorlds()) {
            for (ItemDisplay display : world.getEntitiesByClass(ItemDisplay.class)) {
                boolean legacy = SignItems.isLegacy(display);
                if (items.sideOf(display) != null && items.keepOrRemove(display) && legacy) {
                    adopted++;
                }
            }
        }
        this.items = items;
        scheduleSweep();
        getLogger().info("SHItemSigns prêt (île : " + (islands != null ? "oui" : "non") + ", régions : "
                + (regions != null ? "oui" : "non") + ", objets d'InteractiveSigns repris au démarrage : " + adopted + ").");
    }

    Settings settings() {
        return settings;
    }

    /** Vérification régulière des objets des chunks chargés (relancée à chaque reload). */
    private void scheduleSweep() {
        if (sweepTask != null) {
            sweepTask.cancel();
            sweepTask = null;
        }
        long period = settings.checkTicks;
        if (period > 0) {
            SignItems target = items;
            sweepTask = getServer().getScheduler().runTaskTimer(this, target::sweep, period, period);
        }
    }

    Messages messages() {
        return messages;
    }

    /**
     * Relit config.yml et messages.yml. Les deux sont lus à part et ne remplacent l'état en cours que s'ils sont
     * valides ; sinon l'ancienne configuration reste active et l'exception porte le fichier en cause.
     */
    void reload() throws ReloadException {
        readYaml("config.yml");
        Messages loaded;
        try {
            readYaml("messages.yml");
            loaded = Messages.load(this);
        } catch (IOException | InvalidConfigurationException e) {
            throw new ReloadException("messages.yml", e);
        }
        reloadConfig();
        settings = Settings.load(getConfig());
        messages = loaded;
        if (items != null) {
            scheduleSweep();
        }
    }

    private void readYaml(String name) throws ReloadException {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(new File(getDataFolder(), name));
        } catch (IOException | InvalidConfigurationException e) {
            throw new ReloadException(name, e);
        }
    }

    /** Fichier de configuration illisible pendant un reload. */
    static final class ReloadException extends Exception {

        final String file;

        ReloadException(String file, Throwable cause) {
            super(cause.getMessage() == null ? cause.toString() : cause.getMessage().lines().findFirst().orElse(""), cause);
            this.file = file;
        }
    }
}
