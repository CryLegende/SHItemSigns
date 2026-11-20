package org.Cry_Legende.sHItemSigns;

import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Location;

/** Valeurs de config.yml, lues une fois à chaque chargement. */
final class Settings {

    /** view_range des objets : la distance de config.yml rapportée aux 64 blocs du jeu. */
    final float viewRange;
    /** Taille maximale d'un objet posé, en caractères de sa forme JSON (0 = pas de limite). */
    final int maxItemChars;
    final int maxItemKib;
    final Sound placeSound;
    final Sound takeSound;
    /** Intervalle de vérification des objets des chunks chargés, en ticks (0 = seulement au chargement). */
    final long checkTicks;

    private Settings(FileConfiguration config) {
        double distance = Math.max(1.0, Math.min(256.0, config.getDouble("distance-affichage", 32.0)));
        viewRange = (float) (distance / 64.0);
        maxItemKib = Math.max(0, config.getInt("taille-max-objet", 16));
        maxItemChars = maxItemKib * 1024;
        placeSound = Sound.read(config.getConfigurationSection("sons.poser"));
        takeSound = Sound.read(config.getConfigurationSection("sons.reprendre"));
        checkTicks = Math.round(Math.max(0.0, Math.min(3600.0, config.getDouble("verification-panneaux", 5.0))) * 20.0);
    }

    static Settings load(FileConfiguration config) {
        return new Settings(config);
    }

    /** Un son de config.yml ; une clé vide ne joue rien. */
    record Sound(String key, float volume, float pitch) {

        static Sound read(ConfigurationSection section) {
            if (section == null) {
                return new Sound("", 1f, 1f);
            }
            return new Sound(section.getString("son", "").trim(), (float) section.getDouble("volume", 1.0),
                    (float) section.getDouble("hauteur", 1.0));
        }

        void play(World world, Location where) {
            if (!key.isEmpty()) {
                world.playSound(where, key, SoundCategory.BLOCKS, volume, pitch);
            }
        }
    }
}
