package android.content.res;

import android.annotation.TargetApi;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(Configuration.class)
public class SemConfiguration {

    public int semDisplayDeviceType;
    public static final int SEM_DISPLAY_DEVICE_TYPE_SUB = 5;

    @TargetApi(31)
    public boolean isDexMode() {
        throw new RuntimeException("Stub!");
    }

    @TargetApi(34)
    public boolean isNewDexMode() {
        throw new RuntimeException("Stub!");
    }

}
