package org.Cry_Legende.sHItemSigns;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Clics sur les panneaux, disparition des panneaux et reprise des objets d'InteractiveSigns au chargement des chunks.
 * Chaque événement sort à la première ligne s'il ne concerne pas un panneau (simple test de type de bloc).
 */
final class SignListener implements Listener {

    private final SHItemSigns plugin;
    private final SignItems items;

    SignListener(SHItemSigns plugin, SignItems items) {
        this.plugin = plugin;
        this.items = items;
    }

    // ------------------------------------------------------------------ clics

    /**
     * Après les protections (HIGH) : un clic qu'une île ou une région a déjà refusé (avec son propre message) est
     * laissé tel quel. Sinon le plugin fait sa propre vérification (construire pour poser, casser pour reprendre).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || !Layout.isSign(block.getType()) || event.useInteractedBlock() == Event.Result.DENY) {
            return;
        }
        if (action == Action.LEFT_CLICK_BLOCK) {
            leftClick(event, block);
        } else if (event.getHand() == EquipmentSlot.HAND) {
            rightClick(event, block);
        }
    }

    private void rightClick(PlayerInteractEvent event, Block block) {
        Player player = event.getPlayer();
        // Accroupi : le clic garde son effet normal (poser un bloc contre le panneau...)
        if (player.isSneaking() || !(block.getState(false) instanceof Sign sign)) {
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (sign.isWaxed()) {
            // Panneau ciré : seule une hache fait quelque chose (retirer la cire), le reste est le comportement du jeu
            if (Tag.ITEMS_AXES.isTagged(hand.getType()) && player.hasPermission(SignItems.USE)) {
                event.setCancelled(true);
                if (items.allows(player, block, false)) {
                    items.unwax(block, player);
                } else {
                    plugin.messages().send(player, "protege");
                }
            }
            return;
        }
        // Le panneau mural a l'arrière contre le mur : on ne pose que devant
        Side side = Layout.shape(block.getType()) == Layout.Shape.WALL ? Side.FRONT : sign.getInteractableSideFor(player);
        if (items.on(block, side) != null) {
            // Face occupée : pas d'éditeur ni de bloc posé contre elle ; un rayon de miel cire le panneau
            event.setCancelled(true);
            if (hand.getType() == Material.HONEYCOMB && player.hasPermission(SignItems.USE)) {
                if (items.allows(player, block, false)) {
                    items.wax(block, player, hand);
                } else {
                    plugin.messages().send(player, "protege");
                }
            }
            return;
        }
        if (hand.isEmpty() || hasText(sign.getSide(side)) || !player.hasPermission(SignItems.USE)) {
            return;
        }
        event.setCancelled(true);
        if (!items.allows(player, block, false)) {
            plugin.messages().send(player, "protege");
            return;
        }
        if (items.tooHeavy(hand)) {
            plugin.messages().send(player, "trop-lourd", "max", String.valueOf(plugin.settings().maxItemKib));
            return;
        }
        // L'entité d'abord : si le monde la refuse, l'objet reste dans la main
        if (items.place(block, side, hand.asOne()) == null) {
            return;
        }
        hand.subtract();
        plugin.settings().placeSound.play(block.getWorld(), block.getLocation().add(0.5, 0.5, 0.5));
    }

    private void leftClick(PlayerInteractEvent event, Block block) {
        Player player = event.getPlayer();
        if (!(block.getState(false) instanceof Sign sign)) {
            return;
        }
        ItemDisplay target;
        if (Layout.shape(block.getType()) == Layout.Shape.WALL) {
            // L'arrière d'un panneau mural est inaccessible : il se reprend depuis l'avant, une fois l'avant vide
            target = items.on(block, Side.FRONT);
            if (target == null) {
                target = items.on(block, Side.BACK);
            }
        } else {
            target = items.on(block, sign.getInteractableSideFor(player));
        }
        // Pas d'objet, panneau ciré ou pas la permission : le clic gauche casse le panneau comme d'habitude
        if (target == null || sign.isWaxed() || !player.hasPermission(SignItems.USE)) {
            return;
        }
        event.setCancelled(true);
        if (!items.allows(player, block, true)) {
            plugin.messages().send(player, "protege");
            return;
        }
        items.give(player, target);
        plugin.settings().takeSound.play(block.getWorld(), block.getLocation().add(0.5, 0.5, 0.5));
    }

    private static boolean hasText(SignSide side) {
        for (Component line : side.lines()) {
            if (!PlainTextComponentSerializer.plainText().serialize(line).isBlank()) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ panneau qui disparaît

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        dropIfSign(event.getBlock());
    }

    /** Panneau décroché (bloc qui le tient retiré), cassé par un wither... */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDestroy(BlockDestroyEvent event) {
        dropIfSign(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        dropIfSign(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        dropSigns(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplode(BlockExplodeEvent event) {
        dropSigns(event.blockList());
    }

    /** Un arbre ou un champignon géant qui pousse remplace les blocs de sa liste. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGrow(StructureGrowEvent event) {
        for (BlockState state : event.getBlocks()) {
            Block block = state.getBlock();
            if (Layout.isSign(block.getType()) && state.getType() != block.getType()) {
                items.dropAll(block);
            }
        }
    }

    private void dropSigns(List<Block> blocks) {
        for (Block block : blocks) {
            dropIfSign(block);
        }
    }

    private void dropIfSign(Block block) {
        if (Layout.isSign(block.getType())) {
            items.dropAll(block);
        }
    }

    /** Un panneau reposé là où des objets étaient restés (panneau retiré par WorldEdit...) les reprend. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (Layout.isSign(block.getType())) {
            items.refit(block);
        }
    }

    // ------------------------------------------------------------------ chargement des chunks

    /** Reprend les objets d'InteractiveSigns et règle l'affichage des objets dont la distance a changé. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        int adopted = 0;
        for (Entity entity : event.getEntities()) {
            if (entity instanceof ItemDisplay display) {
                boolean legacy = SignItems.isLegacy(display);
                if (items.sideOf(display) != null) {
                    items.fit(display);
                    if (legacy) {
                        adopted++;
                    }
                }
            }
        }
        if (adopted > 0) {
            plugin.getLogger().info("InteractiveSigns : " + adopted + " objet(s) repris (" + event.getWorld().getName()
                    + ", chunk " + event.getChunk().getX() + " " + event.getChunk().getZ() + ").");
        }
    }
}
