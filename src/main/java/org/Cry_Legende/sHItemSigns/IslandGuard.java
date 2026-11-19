package org.Cry_Legende.sHItemSigns;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.island.IslandPrivilege;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Protection des îles SuperiorSkyblock2 : poser demande la permission d'île BUILD, reprendre la permission BREAK
 * (ce que casser le panneau rendrait de toute façon). Le mode contournement de SuperiorSkyblock2 compte.
 */
final class IslandGuard {

    private final IslandPrivilege build = IslandPrivilege.getByName("BUILD");
    private final IslandPrivilege destroy = IslandPrivilege.getByName("BREAK");

    boolean allows(Player player, Location where, boolean take) {
        Island island = SuperiorSkyblockAPI.getIslandAt(where);
        if (island == null) {
            // Dans le monde des îles, hors de toute île : personne n'y a de droits
            return !SuperiorSkyblockAPI.getGrid().isIslandsWorld(where.getWorld());
        }
        if (island.isSpawn()) {
            // L'île du spawn suit les régions WorldGuard
            return true;
        }
        SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(player);
        return superiorPlayer != null && island.hasPermission(superiorPlayer, take ? destroy : build);
    }
}
