package org.Cry_Legende.sHItemSigns;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Protection des régions WorldGuard : il faut pouvoir construire à l'endroit du panneau (membre de la région,
 * flag build, ou contournement WorldGuard). Hors région, la région globale du monde décide.
 */
final class RegionGuard {

    boolean allows(Player player, Location where) {
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        WorldGuard worldGuard = WorldGuard.getInstance();
        if (worldGuard.getPlatform().getSessionManager().hasBypass(localPlayer, BukkitAdapter.adapt(where.getWorld()))) {
            return true;
        }
        return worldGuard.getPlatform().getRegionContainer().createQuery().testBuild(BukkitAdapter.adapt(where), localPlayer);
    }
}
