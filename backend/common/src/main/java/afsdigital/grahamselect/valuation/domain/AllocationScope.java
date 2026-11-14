package afsdigital.grahamselect.valuation.domain;

import java.util.UUID;

public final class AllocationScope {

    public static final String GLOBAL = "GLOBAL";

    private AllocationScope() {
    }

    public static String fromTagId(UUID tagId) {
        return tagId == null ? GLOBAL : tagId.toString();
    }
}
