package dev.allofus.fusioncore;

import android.content.Intent;

/**
 * This fork targets Meta Quest (Horizon OS) only, so every game is launched as an immersive VR
 * app. There is no per-game VR detection: the selector/bootstrap screens stay 2D panels, and the
 * game itself is always handed off to the system as a VR task.
 *
 * Horizon OS decides "panel vs immersive" when a task is created, from the launching intent's
 * categories and the component's manifest declaration. A game started inside the selector's own
 * panel task therefore stays flat and then crashes as soon as it tries to create an XR session.
 */
public final class VrMode {
    /** Always true in this fork. Kept as a constant so the VR-only assumptions are easy to grep. */
    public static final boolean ENABLED = true;

    public static final String CATEGORY_VR = "com.oculus.intent.category.VR";
    public static final String CATEGORY_2D = "com.oculus.intent.category.2D";

    private VrMode() {
    }

    /** Marks an intent so Horizon OS launches its target as an immersive VR task. */
    public static Intent markImmersive(Intent intent) {
        intent.addCategory(CATEGORY_VR);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        return intent;
    }

    /** Marks an intent so Horizon OS launches its target as a regular 2D panel. */
    public static Intent markPanel(Intent intent) {
        intent.addCategory(CATEGORY_2D);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }
}
