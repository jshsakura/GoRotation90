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

    // mContext 리플렉션을 매 호출 반복하지 않도록 ContentResolver만 1회 캐싱한다.
    // 설정값(ACCELEROMETER_ROTATION)은 SettingsProvider의 세대 카운터 기반
    // 클라이언트 캐시를 타므로 매 호출 읽어도 실질 부하가 거의 없고,
    // 항상 최신값이라 회전이 지연 없이 반영된다.
    private static volatile ContentResolver cachedResolver = null;

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
                            int forcedRotation = isAutoRotateEnabled(param.thisObject) ? ROTATION_0 : ROTATION_90;
                            XposedHelpers.setIntField(param.thisObject, "mUserRotation", forcedRotation);
                        } catch (Throwable t) {
                            XposedBridge.log(TAG + ": Error in updateRotationUnchecked hook: " + t.getMessage());
                        }
                    }
                }
        );
    }

    private boolean isAutoRotateEnabled(Object displayRotation) {
        ContentResolver resolver = cachedResolver;
        if (resolver == null) {
            Context context = (Context) XposedHelpers.getObjectField(displayRotation, "mContext");
            resolver = context.getContentResolver();
            cachedResolver = resolver;
        }
        return Settings.System.getInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1;
    }
}
