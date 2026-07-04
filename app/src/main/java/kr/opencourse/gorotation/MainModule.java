package kr.opencourse.gorotation;

import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import android.view.Surface;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

/**
 * 자동회전 토글을 "회전 트리거"로 사용한다:
 *   자동회전 OFF -> 화면을 가로(ROTATION_90)로 강제
 *   자동회전 ON  -> 화면을 세로(ROTATION_0)로 강제
 *
 * 즉시 반영을 위해 두 가지를 함께 사용한다:
 *   1. rotationForOrientation 후킹 - 회전 계산 결과 자체를 강제 (양방향 확정 동작)
 *   2. ContentObserver - 토글 변경 즉시 WMS.updateRotation()을 호출해 재평가 트리거
 * 설정값은 항상 동기로 읽는다 (SettingsProvider 클라이언트 캐시를 타므로 부하 없음).
 */
public class MainModule implements IXposedHookLoadPackage {
    private static final String TAG = "LGWingRotationControl";
    private static final int ROTATION_90 = Surface.ROTATION_90;
    private static final int ROTATION_0 = Surface.ROTATION_0;

    private static volatile ContentResolver cachedResolver = null;
    private static volatile boolean observerRegistered = false;

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if ("android".equals(lpparam.packageName)) {
            hookWindowManagerService(lpparam.classLoader);
        }
    }

    private void hookWindowManagerService(ClassLoader classLoader) {
        // 회전 계산 결과를 토글 상태에 따라 강제한다.
        // mUserRotation 주입과 달리 자동회전 ON(free 모드)에서도 즉시 세로로 복귀한다.
        XposedHelpers.findAndHookMethod(
                "com.android.server.wm.DisplayRotation",
                classLoader,
                "rotationForOrientation",
                int.class, int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            ensureSetup(param.thisObject);
                            param.setResult(isAutoRotateEnabled() ? ROTATION_0 : ROTATION_90);
                        } catch (Throwable t) {
                            XposedBridge.log(TAG + ": Error in rotationForOrientation hook: " + t.getMessage());
                        }
                    }
                }
        );

        // 회전잠금 모드의 기준값도 함께 맞춰 일관성을 유지한다.
        XposedHelpers.findAndHookMethod(
                "com.android.server.wm.DisplayRotation",
                classLoader,
                "updateRotationUnchecked",
                boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            ensureSetup(param.thisObject);
                            int forcedRotation = isAutoRotateEnabled() ? ROTATION_0 : ROTATION_90;
                            XposedHelpers.setIntField(param.thisObject, "mUserRotation", forcedRotation);
                        } catch (Throwable t) {
                            XposedBridge.log(TAG + ": Error in updateRotationUnchecked hook: " + t.getMessage());
                        }
                    }
                }
        );
    }

    // resolver 캐싱(리플렉션 제거) + 토글 변경 시 즉시 회전 재평가를 트리거할 옵저버 등록
    private static synchronized void ensureSetup(Object displayRotation) {
        if (cachedResolver == null) {
            Context context = (Context) XposedHelpers.getObjectField(displayRotation, "mContext");
            cachedResolver = context.getContentResolver();
        }
        if (!observerRegistered) {
            final Object wms = XposedHelpers.getObjectField(displayRotation, "mService");
            ContentObserver observer = new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    try {
                        // 토글 변경 즉시 회전 재평가 - 다음 회전 이벤트를 기다리지 않는다
                        XposedHelpers.callMethod(wms, "updateRotation", true, false);
                    } catch (Throwable t) {
                        XposedBridge.log(TAG + ": Error triggering updateRotation: " + t.getMessage());
                    }
                }
            };
            cachedResolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION),
                    false, observer);
            observerRegistered = true;
            XposedBridge.log(TAG + ": Rotation trigger observer registered");
        }
    }

    private static boolean isAutoRotateEnabled() {
        return Settings.System.getInt(cachedResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1;
    }
}
