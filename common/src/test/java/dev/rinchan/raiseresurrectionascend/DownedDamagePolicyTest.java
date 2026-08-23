package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class DownedDamagePolicyTest {
    @Test
    void ordinaryHitsKeepTheirPostMitigationDamage() {
        assertEquals(7.0F, DownedDamagePolicy.damageForDeathProtection(20.0F, 1.0F, 7.0F));
        assertEquals(7.0F, DownedDamagePolicy.damageForDeathProtection(13.0F, 1.0F, 7.0F));
    }

    @Test
    void absorptionClearingHitsReachZeroHealthSoVanillaCanCheckTotems() {
        assertEquals(7.0F, DownedDamagePolicy.damageForDeathProtection(6.0F, 1.0F, 7.0F));
        assertEquals(21.0F, DownedDamagePolicy.damageForDeathProtection(20.0F, 1.0F, 20.0F));
        assertEquals(21.0F, DownedDamagePolicy.damageForDeathProtection(20.0F, 1.0F, 21.0F));
    }

    @Test
    void zeroOrNegativeDamageRemainsNonLethal() {
        assertEquals(0.0F, DownedDamagePolicy.damageForDeathProtection(0.0F, 1.0F, 0.0F));
        assertEquals(-1.0F, DownedDamagePolicy.damageForDeathProtection(0.0F, 1.0F, -1.0F));
    }
}
