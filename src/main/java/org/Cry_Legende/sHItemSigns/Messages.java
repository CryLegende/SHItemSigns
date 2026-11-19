package org.Cry_Legende.sHItemSigns;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Textes de messages.yml (codes &amp; et couleurs hex &amp;#RRGGBB). Une clé absente du fichier prend le texte du jar,
 * une clé vide désactive le message.
 */
final class Messages {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder().character('&').hexColors().build();

    private final Map<String, String> texts;

    private Messages(Map<String, String> texts) {
        this.texts = texts;
    }

    /** Lit messages.yml ; lève l'erreur YAML telle quelle pour que l'appelant garde l'ancien état. */
    static Messages load(JavaPlugin plugin) throws IOException, InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(new File(plugin.getDataFolder(), "messages.yml"));
        return merge(yaml, jarDefaults(plugin));
    }

    /** Textes du jar seuls, quand messages.yml est illisible au démarrage. */
    static Messages defaults(JavaPlugin plugin) {
        return merge(new YamlConfiguration(), jarDefaults(plugin));
    }

    private static Messages merge(YamlConfiguration file, YamlConfiguration defaults) {
        Set<String> keys = new LinkedHashSet<>(defaults.getKeys(false));
        keys.addAll(file.getKeys(false));
        Map<String, String> texts = new HashMap<>();
        for (String key : keys) {
            String value = file.isString(key) ? file.getString(key) : defaults.getString(key);
            if (value != null && !value.isEmpty()) {
                texts.put(key, value);
            }
        }
        return new Messages(texts);
    }

    private static YamlConfiguration jarDefaults(JavaPlugin plugin) {
        try (InputStream in = plugin.getResource("messages.yml")) {
            return in == null ? new YamlConfiguration()
                    : YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return new YamlConfiguration();
        }
    }

    /** Envoie le message de la clé s'il existe. Chaque paire de {@code pairs} remplace un {nom} par sa valeur. */
    void send(CommandSender to, String key, String... pairs) {
        String text = texts.get(key);
        if (text == null) {
            return;
        }
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            text = text.replace("{" + pairs[i] + "}", pairs[i + 1]);
        }
        Component message = LEGACY.deserialize(text);
        to.sendMessage(message);
    }
}
