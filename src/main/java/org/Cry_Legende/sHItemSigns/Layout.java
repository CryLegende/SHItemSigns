package org.Cry_Legende.sHItemSigns;

import com.destroystokyo.paper.MaterialTags;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.sign.Side;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Où et comment un objet est dessiné sur un panneau. Les valeurs sont celles d'InteractiveSigns en mode 3D
 * (enable_items_volume: true), pour que les objets posés avant et après le changement de plugin aient la même allure.
 * L'entité est au centre du bloc, tournée vers le panneau ; la transformation place l'objet sur la face.
 */
final class Layout {

    /** Famille de panneau : chaque famille a sa table de positions. */
    enum Shape { STANDING, WALL, HANGING }

    /** Famille d'objet : chaque famille a sa taille et sa rotation sur le panneau. */
    enum Kind { DRAGON_HEAD, HEAD, TALL, BANNER, BED, SHIELD, TRIDENT, ITEM, BLOCK }

    private static final AxisAngle4f NONE = new AxisAngle4f(0f, 0f, 0f, 0f);
    private static final Set<Material> SIGNS = EnumSet.noneOf(Material.class);
    private static final Set<Material> HEADS = EnumSet.of(Material.PLAYER_HEAD, Material.CREEPER_HEAD,
            Material.WITHER_SKELETON_SKULL, Material.SKELETON_SKULL, Material.ZOMBIE_HEAD, Material.PIGLIN_HEAD);
    /** Blocs dessinés à plat comme un objet (torche, pousse, rail...), le reste des blocs l'est en cube. */
    private static final Set<Material> FLAT_BLOCKS = EnumSet.noneOf(Material.class);
    private static final Map<Shape, Map<Kind, Transformation>> FRONT = new EnumMap<>(Shape.class);
    private static final Map<Shape, Map<Kind, Transformation>> BACK = new EnumMap<>(Shape.class);

    static {
        SIGNS.addAll(Tag.ALL_SIGNS.getValues());

        FLAT_BLOCKS.addAll(Tag.DOORS.getValues());
        FLAT_BLOCKS.addAll(Tag.BARS.getValues());
        FLAT_BLOCKS.addAll(Tag.CHAINS.getValues());
        FLAT_BLOCKS.addAll(MaterialTags.GLASS_PANES.getValues());
        FLAT_BLOCKS.addAll(Tag.CANDLES.getValues());
        FLAT_BLOCKS.addAll(Tag.SAPLINGS.getValues());
        FLAT_BLOCKS.addAll(Tag.LANTERNS.getValues());
        FLAT_BLOCKS.addAll(Tag.ALL_SIGNS.getValues());
        FLAT_BLOCKS.addAll(Tag.RAILS.getValues());
        FLAT_BLOCKS.addAll(Tag.CORALS.getValues());
        FLAT_BLOCKS.addAll(EnumSet.of(Material.POINTED_DRIPSTONE, Material.SMALL_AMETHYST_BUD,
                Material.MEDIUM_AMETHYST_BUD, Material.LARGE_AMETHYST_BUD, Material.AMETHYST_CLUSTER,
                Material.BROWN_MUSHROOM, Material.RED_MUSHROOM, Material.CRIMSON_FUNGUS, Material.WARPED_FUNGUS,
                Material.DEAD_BUSH, Material.FERN, Material.BAMBOO, Material.SUGAR_CANE, Material.LARGE_FERN,
                Material.NETHER_SPROUTS, Material.WARPED_ROOTS, Material.CRIMSON_ROOTS, Material.WEEPING_VINES,
                Material.VINE, Material.TWISTING_VINES, Material.HANGING_ROOTS, Material.TURTLE_EGG,
                Material.SNIFFER_EGG, Material.FROGSPAWN, Material.NETHER_WART, Material.SEA_PICKLE, Material.KELP,
                Material.SEAGRASS, Material.LILY_PAD, Material.COBWEB, Material.CAULDRON, Material.TORCH,
                Material.SOUL_TORCH, Material.REDSTONE_TORCH, Material.COPPER_TORCH, Material.CAMPFIRE,
                Material.SOUL_CAMPFIRE, Material.BREWING_STAND, Material.BELL, Material.LADDER, Material.FLOWER_POT,
                Material.REPEATER, Material.COMPARATOR, Material.TRIPWIRE_HOOK, Material.LEVER,
                Material.DEAD_BRAIN_CORAL, Material.DEAD_BRAIN_CORAL_FAN, Material.DEAD_BUBBLE_CORAL,
                Material.DEAD_BUBBLE_CORAL_FAN, Material.DEAD_TUBE_CORAL, Material.DEAD_TUBE_CORAL_FAN,
                Material.DEAD_FIRE_CORAL, Material.DEAD_FIRE_CORAL_FAN, Material.DEAD_HORN_CORAL,
                Material.DEAD_HORN_CORAL_FAN, Material.CAKE));
        FLAT_BLOCKS.removeAll(EnumSet.of(Material.AZALEA, Material.FLOWERING_AZALEA, Material.SPORE_BLOSSOM));

        // Panneau sur pied : les deux faces sont identiques (l'entité est retournée pour l'arrière)
        Map<Kind, Transformation> standing = new EnumMap<>(Kind.class);
        standing.put(Kind.DRAGON_HEAD, plain(0f, 0.38f, -0.046f, 0.4f, 0.4f, 0.2f));
        standing.put(Kind.HEAD, plain(0f, 0.52f, -0.046f, 0.7f, 0.7f, 0.3f));
        standing.put(Kind.TALL, plain(0f, 0.32f, -0.046f, 0.31f, 0.31f, 0.16f));
        standing.put(Kind.BANNER, plain(0f, 0.25f, -0.05f, 0.216f, 0.216f, 0.216f));
        standing.put(Kind.BED, bed(-0.14f, 0.35f, -0.095f, 0.28f, 0.14f, 0.28f));
        standing.put(Kind.SHIELD, shield(-0.14f, 0.485f, -0.17f, 0.28f, 0.28f, 0.26f));
        standing.put(Kind.TRIDENT, trident(-0.28f, -0.13f, 0.1f));
        standing.put(Kind.ITEM, item(0f, 0.34f, -0.05f, 0.4f, 0.4f, 0.5f));
        standing.put(Kind.BLOCK, plain(0f, 0.34f, -0.048f, 0.35f, 0.35f, 0.175f));
        FRONT.put(Shape.STANDING, standing);
        BACK.put(Shape.STANDING, standing);

        // Panneau mural : l'avant est contre la planche, l'arrière (côté mur) n'est plus utilisé pour poser
        Map<Kind, Transformation> wallFront = new EnumMap<>(Kind.class);
        wallFront.put(Kind.DRAGON_HEAD, plain(0f, 0.08f, 0.389f, 0.4f, 0.4f, 0.2f));
        wallFront.put(Kind.HEAD, plain(0f, 0.21f, 0.389f, 0.7f, 0.7f, 0.35f));
        wallFront.put(Kind.TALL, plain(0f, 0.01f, 0.389f, 0.31f, 0.31f, 0.16f));
        wallFront.put(Kind.BANNER, plain(0f, -0.07f, 0.389f, 0.216f, 0.216f, 0.216f));
        wallFront.put(Kind.BED, bed(-0.14f, 0.048f, 0.332f, 0.28f, 0.14f, 0.28f));
        wallFront.put(Kind.SHIELD, shield(-0.14f, 0.175f, 0.265f, 0.28f, 0.28f, 0.26f));
        wallFront.put(Kind.TRIDENT, trident(-0.28f, -0.42f, 0.52f));
        wallFront.put(Kind.ITEM, item(0f, 0.03f, 0.384f, 0.4f, 0.4f, 0.4f));
        wallFront.put(Kind.BLOCK, plain(0f, 0.03f, 0.389f, 0.35f, 0.35f, 0.14f));
        FRONT.put(Shape.WALL, wallFront);
        Map<Kind, Transformation> wallBack = new EnumMap<>(Kind.class);
        wallBack.put(Kind.DRAGON_HEAD, plain(0f, 0.08f, -0.483f, 0.4f, 0.4f, 0.2f));
        wallBack.put(Kind.HEAD, plain(0f, 0.21f, -0.483f, 0.7f, 0.7f, 0.35f));
        wallBack.put(Kind.TALL, plain(0f, 0.01f, -0.483f, 0.31f, 0.31f, 0.16f));
        wallBack.put(Kind.BANNER, plain(0f, -0.07f, -0.483f, 0.216f, 0.216f, 0.216f));
        wallBack.put(Kind.BED, bed(-0.14f, 0.048f, -0.54f, 0.28f, 0.14f, 0.28f));
        wallBack.put(Kind.SHIELD, shield(-0.14f, 0.175f, -0.612f, 0.28f, 0.28f, 0.26f));
        wallBack.put(Kind.TRIDENT, trident(-0.28f, -0.42f, -0.33f));
        wallBack.put(Kind.ITEM, item(0f, 0.03f, -0.488f, 0.4f, 0.4f, 0.4f));
        wallBack.put(Kind.BLOCK, plain(0f, 0.03f, -0.483f, 0.35f, 0.35f, 0.14f));
        BACK.put(Shape.WALL, wallBack);

        // Panneau suspendu (au plafond ou à un mur) : les deux faces sont identiques
        Map<Kind, Transformation> hanging = new EnumMap<>(Kind.class);
        hanging.put(Kind.DRAGON_HEAD, plain(0f, -0.14f, -0.06451f, 0.5f, 0.5f, 0.25f));
        hanging.put(Kind.HEAD, plain(0f, 0f, -0.06451f, 0.8f, 0.8f, 0.4f));
        hanging.put(Kind.TALL, plain(0f, -0.21f, -0.06451f, 0.4f, 0.4f, 0.25f));
        hanging.put(Kind.BANNER, plain(0f, -0.31f, -0.072f, 0.27f, 0.27f, 0.27f));
        hanging.put(Kind.BED, bed(-0.177f, -0.18f, -0.15f, 0.35f, 0.35f, 0.35f));
        hanging.put(Kind.SHIELD, shield(-0.175f, 0f, -0.225f, 0.35f, 0.35f, 0.35f));
        hanging.put(Kind.TRIDENT, trident(-0.25f, -0.6f, 0.12f));
        hanging.put(Kind.ITEM, item(0f, -0.18f, -0.074f, 0.5f, 0.5f, 0.5f));
        hanging.put(Kind.BLOCK, plain(0f, -0.18f, -0.06451f, 0.45f, 0.45f, 0.225f));
        FRONT.put(Shape.HANGING, hanging);
        BACK.put(Shape.HANGING, hanging);
    }

    private Layout() {
    }

    static boolean isSign(Material material) {
        return SIGNS.contains(material);
    }

    static Shape shape(Material sign) {
        if (Tag.WALL_SIGNS.isTagged(sign)) {
            return Shape.WALL;
        }
        if (Tag.WALL_HANGING_SIGNS.isTagged(sign) || Tag.CEILING_HANGING_SIGNS.isTagged(sign)) {
            return Shape.HANGING;
        }
        return Shape.STANDING;
    }

    static Kind kind(Material item) {
        if (item == Material.DRAGON_HEAD) {
            return Kind.DRAGON_HEAD;
        }
        if (HEADS.contains(item)) {
            return Kind.HEAD;
        }
        if (item == Material.DECORATED_POT || item == Material.LECTERN) {
            return Kind.TALL;
        }
        if (Tag.BANNERS.isTagged(item)) {
            return Kind.BANNER;
        }
        if (Tag.BEDS.isTagged(item)) {
            return Kind.BED;
        }
        if (item == Material.SHIELD) {
            return Kind.SHIELD;
        }
        if (item == Material.TRIDENT) {
            return Kind.TRIDENT;
        }
        if (FLAT_BLOCKS.contains(item) || !item.isBlock()) {
            return Kind.ITEM;
        }
        return Kind.BLOCK;
    }

    static Transformation transformation(Shape shape, Side side, Material item) {
        return (side == Side.BACK ? BACK : FRONT).get(shape).get(kind(item));
    }

    /** Position de l'entité : centre du bloc, tournée vers la planche pour l'avant, vers l'extérieur pour l'arrière. */
    static Location anchor(Block sign, Side side) {
        BlockData data = sign.getBlockData();
        Material material = data.getMaterial();
        boolean onWall = Tag.WALL_SIGNS.isTagged(material) || Tag.WALL_HANGING_SIGNS.isTagged(material);
        BlockFace face = onWall ? ((Directional) data).getFacing() : ((Rotatable) data).getRotation().getOppositeFace();
        Vector facing = direction(face);
        if (onWall) {
            facing.multiply(-1);
        }
        Location location = sign.getLocation().add(0.5, 0.5, 0.5);
        location.setDirection(side == Side.FRONT ? facing.multiply(-1) : facing);
        return location;
    }

    private static Vector direction(BlockFace face) {
        double angle = switch (face) {
            case NORTH_NORTH_EAST -> 22.5;
            case NORTH_EAST -> 45.0;
            case EAST_NORTH_EAST -> 67.5;
            case EAST -> 90.0;
            case EAST_SOUTH_EAST -> 112.5;
            case SOUTH_EAST -> 135.0;
            case SOUTH_SOUTH_EAST -> 157.5;
            case SOUTH -> 180.0;
            case SOUTH_SOUTH_WEST -> -157.5;
            case SOUTH_WEST -> -135.0;
            case WEST_SOUTH_WEST -> -112.5;
            case WEST -> -90.0;
            case WEST_NORTH_WEST -> -67.5;
            case NORTH_WEST -> -45.0;
            case NORTH_NORTH_WEST -> -22.5;
            default -> 0.0;
        };
        return new Vector(Math.sin(-Math.toRadians(angle)), 0.0, Math.cos(Math.toRadians(angle)));
    }

    private static Transformation plain(float x, float y, float z, float sx, float sy, float sz) {
        return new Transformation(new Vector3f(x, y, z), NONE, new Vector3f(sx, sy, sz), NONE);
    }

    private static Transformation bed(float x, float y, float z, float sx, float sy, float sz) {
        return new Transformation(new Vector3f(x, y, z), new AxisAngle4f(1.572f, -1f, 0f, 0f),
                new Vector3f(sx, sy, sz), new AxisAngle4f(1.572f, 0f, 1f, 0f));
    }

    private static Transformation shield(float x, float y, float z, float sx, float sy, float sz) {
        return new Transformation(new Vector3f(x, y, z), new AxisAngle4f(0f, -1f, 0f, 0f),
                new Vector3f(sx, sy, sz), new AxisAngle4f(0f, 0f, 1f, 0f));
    }

    private static Transformation trident(float x, float y, float z) {
        return new Transformation(new Vector3f(x, y, z), new AxisAngle4f(1.8f, 1f, 0f, 0f),
                new Vector3f(0.75f, 0.75f, 0.75f), new AxisAngle4f(1.8f, 0f, 1f, 0f));
    }

    private static Transformation item(float x, float y, float z, float sx, float sy, float sz) {
        return new Transformation(new Vector3f(x, y, z), new AxisAngle4f(0f, -1f, 0f, 0f),
                new Vector3f(sx, sy, sz), new AxisAngle4f(3.144f, 0f, 1f, 0f));
    }
}
