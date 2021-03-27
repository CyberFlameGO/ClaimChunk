package com.cjburkey.claimchunk.config.spread;

import com.cjburkey.claimchunk.config.ccconfig.CCConfig;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public class SpreadProfile {

    public final String key;
    private final String str_fromClaimedIntoDiffClaimed;
    private final String str_fromClaimedIntoUnclaimed;
    private final String str_fromUnclaimedIntoClaimed;

    public boolean fromClaimedIntoDiffClaimed = true;
    public boolean fromClaimedIntoUnclaimed = true;
    public boolean fromUnclaimedIntoClaimed = true;

    public SpreadProfile(@Nonnull String key) {
        this.key = key;

        str_fromClaimedIntoDiffClaimed = key + ".from_claimed.into_diff_claimed";
        str_fromClaimedIntoUnclaimed = key + ".from_claimed.into_unclaimed";
        str_fromUnclaimedIntoClaimed = key + ".from_unclaimed.into_claimed";
    }

    public boolean getShouldCancel(@Nullable UUID sourceOwner,
                                   @Nullable UUID newOwner) {
        // Full spread profile handles same-owner spreads
        if (!Objects.equals(sourceOwner, newOwner)) {
            // Disable block spread from unclaimed chunks into claimed chunks
            if (!fromUnclaimedIntoClaimed && sourceOwner == null) return true;

            // Disable block spread from claimed chunks into different claimed chunks
            if (!fromClaimedIntoDiffClaimed && newOwner != null && sourceOwner != null) return true;

            // Disable block spread from claimed chunks into unclaimed chunks
            //noinspection RedundantIfStatement
            if (!fromClaimedIntoUnclaimed && sourceOwner != null) return true;
        }

        // No need to cancel
        return false;
    }

    public void toCCConfig(CCConfig config) {
        config.set(str_fromClaimedIntoDiffClaimed, fromClaimedIntoDiffClaimed);
        config.set(str_fromClaimedIntoUnclaimed, fromClaimedIntoUnclaimed);
        config.set(str_fromUnclaimedIntoClaimed, fromUnclaimedIntoClaimed);
    }

    public void fromCCConfig(CCConfig config) {
        fromClaimedIntoDiffClaimed = config.getBool(str_fromClaimedIntoDiffClaimed, fromClaimedIntoDiffClaimed);
        fromClaimedIntoUnclaimed = config.getBool(str_fromClaimedIntoUnclaimed, fromClaimedIntoUnclaimed);
        fromUnclaimedIntoClaimed = config.getBool(str_fromUnclaimedIntoClaimed, fromUnclaimedIntoClaimed);
    }

}
