package dev.allofus.fusioncore;

/**
 * VR variant of {@link StubActivity}.
 *
 * Used only when the target game is detected as a VR app. Its manifest entry declares
 * the {@code com.oculus.intent.category.VR} intent category, which is what makes Meta
 * Quest launch the hosted game in immersive VR mode. Without it, Quest launches the
 * game as a flat 2D panel and VR Unity games crash when their XR stack initializes.
 *
 * The instrumentation hooks ({@code InstrumentationHooks}) replace this stub with the
 * game's real activity class exactly like they do for {@link StubActivity}, so the
 * game still runs inside FusionCore's process with all hooks applied.
 */
public class StubActivityVr extends StubActivity {
    // All behavior is inherited; the difference is purely the manifest declaration.
}
