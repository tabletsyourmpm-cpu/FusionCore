package dev.allofus.fusioncore;

import android.content.Intent;
import android.os.Bundle;

import dev.allofus.fusioncore.hooks.InstrumentationHooks;

public class StubActivity extends android.app.Activity {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(null);
        // Normally GameInstrumentation swaps this activity for the game's real one before we get
        // here. If we are reached without a dynamic intent (e.g. Horizon relaunching this VR task
        // from Recents after the process died), send the user back to the 2D selector and get out
        // of the VR task instead of leaving an empty immersive window behind.
        if (!getIntent().getBooleanExtra(InstrumentationHooks.EXTRA_IS_DYNAMIC_ACTIVITY, false)) {
            startActivity(VrMode.markPanel(new Intent(this, SelectorActivity.class)));
            finish();
        }
    }

    @Override
    protected void onPause() {
        // If we're about to be replaced by another activity, don't pause unnecessarily
        super.onPause();
    }
}
