package android.view;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(WindowManager.class)
public interface SemWindowManager {

    public static class LayoutParams {
        public static final int SEM_EXTENSION_FLAG_RESIZE_FULLSCREEN_WINDOW_ON_SOFT_INPUT = 1;

        public final void semAddExtensionFlags(int flags) {
            throw new RuntimeException("Stub!");
        }

        public final void semClearExtensionFlags(int flags) {
            throw new RuntimeException("Stub!");
        }
    }

}
