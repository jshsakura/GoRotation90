package kr.opencourse.gorotation;

import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import android.view.Surface;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

public class MainModule implements IXposedHookLoadPackage {
    private static final String TAG = "LGWingRotationControl";
    private static final int ROTATION_90 = Surface.ROTATION_90;
    private static final int ROTATION_0 = Surface.ROTATION_0;

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if ("android".equals(lpparam.packageName)) {
            hookWindowManagerService(lpparam.classLoader);
        }
    }

    private void hookWindowManagerService(ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod(
                "com.android.server.wm.DisplayRotation",
                classLoader,
                "updateRotationUnchecked",
                boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                            ContentResolver resolver = context.getContentResolver();

                            int autoRotateSetting = Settings.System.getInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 0);
                            boolean isAutoRotateEnabled = (autoRotateSetting == 1);

                            int forcedRotation = isAutoRotateEnabled ? ROTATION_0 : ROTATION_90;
                            XposedHelpers.setIntField(param.thisObject, "mUserRotation", forcedRotation);
                        } catch (Throwable t) {
                            XposedBridge.log(TAG + ": Error in updateRotationUnchecked hook: " + t.getMessage());
                        }
                    }
                }
        );
    }
}