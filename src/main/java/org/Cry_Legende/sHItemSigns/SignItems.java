package org.Cry_Legende.sHItemSigns;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Les objets posés sur les panneaux. Un objet est un item_display au centre du bloc du panneau, marqué dans ses
 * données (PDC) par la face qu'il occupe : rien d'autre n'est stocké, le panneau et l'objet se retrouvent par leur
 * position. Tout se fait dans l'événement, sur le thread du monde : retirer l'entité et rendre l'objet arrivent dans
 * le même tick, l'objet ne peut donc ressortir qu'une fois.
 */
final class SignItems {

    static final String USE = "shitemsigns.use";
    static final String BYPASS = "shitemsigns.bypass";

    /** Marque d'InteractiveSigns : NamespacedKey.fromString("interactivesigns.front") tombe dans l'espace minecraft. */
    private static final NamespacedKey OLD_FRONT = NamespacedKey.minecraft("interactivesigns.front");
    private static final NamespacedKey OLD_BACK = NamespacedKey.minecraft("interactivesigns.back");

    /**
     * Boîte de culling (largeur et hauteur de l'entité, vues par le client seulement). Le client l'élargit encore
     * d'un demi-bloc de chaque côté : 1 couvre tout le bloc du panneau et ce qui en dépasse. À 0 (InteractiveSigns),
     * le client dessine l'objet même hors de l'écran.
     */
    private static final float BOX = 1.0f;

    private final SHItemSigns plugin;
    private final NamespacedKey sideKey;
    private final IslandGuard islands;
    private final RegionGuard regions;
    private volatile boolean guardFailureLogged;

    SignItems(SHItemSigns plugin, IslandGuard islands, RegionGuard regions) {
        this.plugin = plugin;
        this.sideKey = new NamespacedKey(plugin, "side");
        this.islands = islands;
        this.regions = regions;
    }

    // ------------------------------------------------------------------ données

    /**
     * Face occupée par cet objet, ou null si l'entité n'est pas un objet de panneau. Un objet posé par
     * InteractiveSigns est repris au passage (marque convertie, affichage réglé), sans toucher à l'objet lui-même.
     */
    Side sideOf(ItemDisplay display) {
        PersistentDataContainer data = display.getPersistentDataContainer();
        String side = data.get(sideKey, PersistentDataType.STRING);
        if (side != null) {
            return "back".equals(side) ? Side.BACK : Side.FRONT;
        }
        return adopt(display, data);
    }

    /** Vrai si l'entité vient d'InteractiveSigns et n'a pas encore été reprise. */
    static boolean isLegacy(ItemDisplay display) {
        PersistentDataContainer data = display.getPersistentDataContainer();
        return data.has(OLD_FRONT) || data.has(OLD_BACK);
    }

    private Side adopt(ItemDisplay display, PersistentDataContainer data) {
        boolean front = data.has(OLD_FRONT);
        if (!front && !data.has(OLD_BACK)) {
            return null;
        }
        Side side = front ? Side.FRONT : Side.BACK;
        data.set(sideKey, PersistentDataType.STRING, front ? "front" : "back");
        data.remove(OLD_FRONT);
        data.remove(OLD_BACK);
        fit(display);
        return side;
    }

    /** Boîte de culling et distance d'affichage, seulement si elles diffèrent (sinon rien n'est renvoyé aux joueurs). */
    void fit(ItemDisplay display) {
        if (display.getDisplayWidth() != BOX) {
            display.setDisplayWidth(BOX);
        }
        if (display.getDisplayHeight() != BOX) {
            display.setDisplayHeight(BOX);
        }
        float range = plugin.settings().viewRange;
        if (display.getViewRange() != range) {
            display.setViewRange(range);
        }
    }

    /** Objets de ce panneau, toutes faces confondues. */
    List<ItemDisplay> on(Block sign) {
        List<ItemDisplay> found = new ArrayList<>(2);
        for (ItemDisplay display : sign.getWorld().getNearbyEntitiesByType(ItemDisplay.class, center(sign), 0.1)) {
            if (display.isValid() && sideOf(display) != null) {
                found.add(display);
            }
        }
        return found;
    }

    /** Objet posé sur cette face du panneau, ou null. */
    ItemDisplay on(Block sign, Side side) {
        for (ItemDisplay display : sign.getWorld().getNearbyEntitiesByType(ItemDisplay.class, center(sign), 0.1)) {
            if (display.isValid() && sideOf(display) == side) {
                return display;
            }
        }
        return null;
    }

    private static Location center(Block block) {
        return block.getLocation().add(0.5, 0.5, 0.5);
    }

    // ------------------------------------------------------------------ actions

    /** Pose une copie de l'objet (un exemplaire). Rend l'entité, ou null si le monde l'a refusée. */
    ItemDisplay place(Block sign, Side side, ItemStack one) {
        Location at = Layout.anchor(sign, side);
        var transformation = Layout.transformation(Layout.shape(sign.getType()), side, one.getType());
        float range = plugin.settings().viewRange;
        ItemDisplay display = sign.getWorld().spawn(at, ItemDisplay.class, entity -> {
            // Tout est réglé avant l'ajout au monde : les joueurs reçoivent l'entité complète en une fois
            entity.setItemStack(one);
            entity.setTransformation(transformation);
            entity.setDisplayWidth(BOX);
            entity.setDisplayHeight(BOX);
            entity.setViewRange(range);
            entity.getPersistentDataContainer().set(sideKey, PersistentDataType.STRING,
                    side == Side.BACK ? "back" : "front");
        });
        return display.isValid() ? display : null;
    }

    /** Retire l'objet du panneau et le met dans l'inventaire (le surplus tombe aux pieds du joueur). */
    void give(Player player, ItemDisplay display) {
        ItemStack stack = display.getItemStack();
        display.remove();
        if (stack.isEmpty()) {
            return;
        }
        for (ItemStack rest : player.getInventory().addItem(stack).values()) {
            player.getWorld().dropItem(player.getLocation(), rest, item -> item.setOwner(player.getUniqueId()));
        }
    }

    /** Le panneau disparaît : chaque objet tombe au centre du bloc. Rend le nombre d'objets lâchés. */
    int dropAll(Block sign) {
        List<ItemDisplay> displays = on(sign);
        Location center = center(sign);
        for (ItemDisplay display : displays) {
            ItemStack stack = display.getItemStack();
            display.remove();
            if (!stack.isEmpty()) {
                sign.getWorld().dropItemNaturally(center, stack);
            }
        }
        return displays.size();
    }

    /** Un panneau vient d'être posé là où des objets attendaient : ils prennent l'orientation du nouveau panneau. */
    void refit(Block sign) {
        Layout.Shape shape = Layout.shape(sign.getType());
        for (ItemDisplay display : on(sign)) {
            Side side = sideOf(display);
            Location at = Layout.anchor(sign, side);
            display.setRotation(at.getYaw(), at.getPitch());
            display.setTransformation(Layout.transformation(shape, side, display.getItemStack().getType()));
        }
    }

    /** Cire le panneau avec le rayon de miel tenu en main (non consommé en créatif), comme le jeu. */
    void wax(Block block, Player player, ItemStack honeycomb) {
        if (!(block.getState() instanceof Sign sign)) {
            return;
        }
        sign.setWaxed(true);
        sign.update(true, false);
        if (player.getGameMode() != GameMode.CREATIVE) {
            honeycomb.subtract();
        }
        block.getWorld().playSound(center(block), "minecraft:item.honeycomb.wax_on", SoundCategory.BLOCKS, 1f, 1f);
        block.getWorld().playEffect(block.getLocation(), Effect.COPPER_WAX_ON, 0);
    }

    /** Retire la cire avec la hache tenue en main, qui perd un point de solidité. */
    void unwax(Block block, Player player) {
        if (!(block.getState() instanceof Sign sign)) {
            return;
        }
        sign.setWaxed(false);
        sign.update(true, false);
        if (player.getGameMode() != GameMode.CREATIVE) {
            player.damageItemStack(EquipmentSlot.HAND, 1);
        }
        block.getWorld().playSound(center(block), "minecraft:item.axe.wax_off", SoundCategory.BLOCKS, 1f, 1f);
        block.getWorld().playEffect(block.getLocation(), Effect.COPPER_WAX_OFF, 0);
    }

    // ------------------------------------------------------------------ règles

    /** Vrai si le joueur peut poser (take = false) ou reprendre (take = true) un objet sur ce panneau. */
    boolean allows(Player player, Block sign, boolean take) {
        if (player.hasPermission(BYPASS)) {
            return true;
        }
        if (player.getGameMode() == GameMode.ADVENTURE) {
            return false;
        }
        Location where = sign.getLocation();
        try {
            if (islands != null && !islands.allows(player, where, take)) {
                return false;
            }
            return regions == null || regions.allows(player, where);
        } catch (RuntimeException | LinkageError e) {
            // Une protection qui échoue refuse : mieux vaut un clic sans effet qu'un objet pris chez quelqu'un
            if (!guardFailureLogged) {
                guardFailureLogged = true;
                plugin.getLogger().log(Level.WARNING, "Vérification de protection impossible, action refusée", e);
            }
            return false;
        }
    }

    /** Vrai si l'objet dépasse la taille permise (forme JSON, celle qui part chez chaque joueur à portée). */
    boolean tooHeavy(ItemStack item) {
        int max = plugin.settings().maxItemChars;
        if (max <= 0) {
            return false;
        }
        try {
            return Bukkit.getUnsafe().serializeItemAsJson(item).toString().length() > max;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
