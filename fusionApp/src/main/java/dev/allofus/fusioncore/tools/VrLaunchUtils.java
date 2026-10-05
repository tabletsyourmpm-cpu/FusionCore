package dev.allofus.fusioncore.tools;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import java.util.List;

import dev.allofus.fusioncore.FusionSettings;
import dev.allofus.fusioncore.R;
import dev.allofus.fusioncore.StubActivity;
import dev.allofus.fusioncore.StubActivityVr;

/**
 * Shared VR-launch helpers used by BootstrapActivity (real launches) and
 * SelectorActivity (the "VR Test" diagnostics button), so both always agree
 * on how a game will be launched.
 */
public final class VrLaunchUtils {

    private static final String TAG = "VrLaunch";

    /**
     * Oculus/Meta intent category marking an activity as an immersive VR app.
     * Quest only enters immersive VR mode for activities launched with this category;
     * without it the game opens as a flat 2D panel (or the launch is dropped).
     */
    public static final String VR_INTENT_CATEGORY = "com.oculus.intent.category.VR";

    private VrLaunchUtils() {
    }

    /**
     * Resolves the game's launcher activity the same way BootstrapActivity does,
     * honoring the per-game activity override when one is set.
     */
    public static ComponentName resolveLauncherComponent(Context context, String targetPackage) {
        PackageManager pm = context.getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(targetPackage);
        if (launchIntent == null) {
            return null;
        }
        ComponentName component = launchIntent.getComponent();
        if (component == null) {
            component = launchIntent.resolveActivity(pm);
        }
        if (component == null) {
            return null;
        }

        try {
            String overrideActivity = FusionSettings.getActivityOverrideForGame(context, targetPackage);
            if (!overrideActivity.equals(context.getString(R.string.settings_automatic))) {
                Context gameContext = context.createPackageContext(
                        targetPackage,
                        Context.CONTEXT_IGNORE_SECURITY | Context.CONTEXT_INCLUDE_CODE);
                if (gameContext.getClassLoader().loadClass(overrideActivity) != null) {
                    return new ComponentName(targetPackage, overrideActivity);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Activity override lookup failed for " + targetPackage, e);
        }
        return component;
    }

    /**
     * True when the target's launcher activity declares the Oculus VR intent category.
     */
    public static boolean isVrGame(PackageManager pm, String targetPackage, ComponentName launcherComponent) {
        if (launcherComponent == null) {
            return false;
        }
        try {
            Intent vrQuery = new Intent(Intent.ACTION_MAIN);
            vrQuery.addCategory(VR_INTENT_CATEGORY);
            vrQuery.setPackage(targetPackage);
            List<ResolveInfo> vrActivities = pm.queryIntentActivities(vrQuery, PackageManager.MATCH_ALL);
            for (ResolveInfo info : vrActivities) {
                if (info.activityInfo == null) {
                    continue;
                }
                // ComponentName resolves relative class names (".UnityPlayerActivity") correctly.
                ComponentName vrComponent =
                        new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
                if (vrComponent.equals(launcherComponent)) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "VR detection failed for " + targetPackage, e);
        }
        return false;
    }

    /**
     * Which stub activity will host the game.
     */
    public static Class<?> stubFor(boolean isVrGame) {
        return isVrGame ? StubActivityVr.class : StubActivity.class;
    }

    /**
     * Builds a human-readable diagnostic of exactly how a game would be launched.
     * Used by the "VR Test" button; also logged via FusionLogger.
     */
    public static String describeLaunchPlan(Context context, String targetPackage) {
        StringBuilder out = new StringBuilder();
        out.append("Package: ").append(targetPackage).append("\n");

        ComponentName launcher = resolveLauncherComponent(context, targetPackage);
        if (launcher == null) {
            out.append("Launcher activity: <could not resolve>\n");
            out.append("Result: CANNOT LAUNCH (no launch intent)\n");
            return out.toString();
        }
        out.append("Launcher activity: ").append(launcher.flattenToShortString()).append("\n");

        boolean vr = isVrGame(context.getPackageManager(), targetPackage, launcher);
        out.append("VR category detected: ").append(vr ? "YES" : "NO").append("\n");
        out.append("Stub activity: ").append(stubFor(vr).getName()).append("\n");
        out.append("Launch mode: ").append(vr ? "IMMERSIVE VR" : "flat 2D").append("\n");

        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(targetPackage);
        if (launchIntent != null && launchIntent.getCategories() != null) {
            out.append("Original intent categories: ").append(launchIntent.getCategories()).append("\n");
        } else {
            out.append("Original intent categories: (none)\n");
        }
        if (vr) {
            out.append("VR category will be added to launch intent: YES\n");
        }
        out.append("Orientation: ").append(vr ? "skipped (VR)" : "forced from game manifest").append("\n");

        String override = FusionSettings.getActivityOverrideForGame(context, targetPackage);
        out.append("Activity override: ").append(override).append("\n");
        return out.toString();
    }
}
