package com.cjburkey.claimchunk.worldguard;

import com.cjburkey.claimchunk.ClaimChunk;
import com.cjburkey.claimchunk.Utils;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Chunk;

/**
 * THIS CLASS IS A RAW IMPLEMENTATION
 * IT WILL CRASH IF WORLD GUARD IS NOT PRESENT
 * USE {@link com.cjburkey.claimchunk.worldguard.WorldGuardHandler} instead
 */
class WorldGuardApi {

    private static final String CHUNK_CLAIM_FLAG_NAME = "chunk-claim";
    private static StateFlag FLAG_CHUNK_CLAIM;

    static boolean _init(ClaimChunk claimChunk) {
        FLAG_CHUNK_CLAIM = new StateFlag(CHUNK_CLAIM_FLAG_NAME,
                claimChunk.chConfig().getAllowClaimsInWGRegionsByDefault());

        try {
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            registry.register(FLAG_CHUNK_CLAIM);
            return true;
        } catch (FlagConflictException ignored) {
            Utils.log("Flag \"%s\" is already registered with WorldGuard", CHUNK_CLAIM_FLAG_NAME);
            // If the flag is already registered, that's ok, we can carry on
            return true;
        } catch (Exception e) {
            Utils.err("Failed to initialize WorldGuard support");
            e.printStackTrace();
        }
        return false;
    }

    static boolean _isAllowedClaim(ClaimChunk claimChunk, Chunk chunk) {
        try {
            // Generate a region in the given chunk to get all intersecting regions
            int bx = chunk.getX() << 4;
            int bz = chunk.getZ() << 4;
            BlockVector3 pt1 = BlockVector3.at(bx, 0, bz);
            BlockVector3 pt2 = BlockVector3.at(bx + 15, 256, bz + 15);
            ProtectedCuboidRegion region = new ProtectedCuboidRegion("_", pt1, pt2);
            RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(chunk.getWorld()));

            // No regions in this world, claiming should be determined by the config
            if (regionManager == null) {
                return claimChunk.chConfig().getAllowClaimingInNonWGWorlds();
            }

            // If any regions in the given chunk deny chunk claiming, false is returned
            for (ProtectedRegion regionIn : regionManager.getApplicableRegions(region)) {
                StateFlag.State flag = regionIn.getFlag(FLAG_CHUNK_CLAIM);
                if (flag == StateFlag.State.DENY) return false;
            }

            // No objections
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // An error occurred, better to be on the safe side so false is returned
        return false;
    }

}
