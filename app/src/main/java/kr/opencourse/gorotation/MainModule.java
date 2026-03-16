package kr.opencourse.gorotation;

import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import android.view.Surface;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.provider.Settings;
import android.os.Handler;

public class MainModule implements IXposedHookLoadPackage {
    private static final String TAG = "LGWingRotationControl";
    private static final int ROTATION_90 = Surface.ROTATION_90;
    private static final int ROTATION_0 = Surface.ROTATION_0;

    // ContentObserver 방식 - 설정 변경 시에만 읽어서 발열 최소화
    private static volatile boolean cachedAutoRotateEnabled = false;
    private static ContentObserver rotationObserver = null;

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
                            // 첫 호출 시 ContentObserver 등록
                            if (rotationObserver == null) {
                                Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                                ContentResolver resolver = context.getContentResolver();

                                // 초기값 읽기
                                int autoRotateSetting = Settings.System.getInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 0);
                                cachedAutoRotateEnabled = (autoRotateSetting == 1);

                                // ContentObserver 등록
                                rotationObserver = new ContentObserver(new Handler()) {
                                    @Override
                                    public void onChange(boolean selfChange) {
                                        super.onChange(selfChange);
                                        try {
                                            int setting = Settings.System.getInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 0);
                                            cachedAutoRotateEnabled = (setting == 1);
                                        } catch (Exception e) {
                                            XposedBridge.log(TAG + ": Error reading rotation setting: " + e.getMessage());
                                        }
                                    }
                                };

                                Uri rotationUri = Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION);
                                resolver.registerContentObserver(rotationUri, false, rotationObserver);
                            }

                            int forcedRotation = cachedAutoRotateEnabled ? ROTATION_0 : ROTATION_90;
                            XposedHelpers.setIntField(param.thisObject, "mUserRotation", forcedRotation);
                        } catch (Throwable t) {
                            XposedBridge.log(TAG + ": Error in updateRotationUnchecked hook: " + t.getMessage());
                        }
                    }
                }
        );
    }
}