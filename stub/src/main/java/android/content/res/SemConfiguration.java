package android.content.res;

import android.annotation.TargetApi;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(Configuration.class)
public class SemConfiguration {

    @TargetApi(31)
    public boolean isDexMode() {
        throw new RuntimeException("Stub!");
    }

    @TargetApi(34)
    public boolean isNewDexMode() {
        throw new RuntimeException("Stub!");
    }

}
