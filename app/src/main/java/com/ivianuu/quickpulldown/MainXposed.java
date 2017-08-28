package com.ivianuu.quickpulldown;

import android.view.MotionEvent;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setBooleanField;

/**
 * Main Xposed
 */
public class MainXposed implements IXposedHookLoadPackage {

    private static final String NOTIFICATION_PANEL_VIEW = "com.android.systemui.statusbar.phone.NotificationPanelView";
    private static final String SYSTEM_UI = "com.android.systemui";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(SYSTEM_UI)) {
            // were only interested in the system ui package
            return;
        }

        Class<?> notificationPanelViewClass = findClass(NOTIFICATION_PANEL_VIEW, lpparam.classLoader);

        // hook touch events
        findAndHookMethod(notificationPanelViewClass,
                "onTouchEvent", MotionEvent.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        final Object thiz = param.thisObject;

                        final MotionEvent event = (MotionEvent) param.args[0];
                        boolean quickPulldown = event.getActionMasked() == MotionEvent.ACTION_DOWN
                                && shouldQuickSettingsIntercept(thiz, event.getX(), event.getY(), -1)
                                && event.getY(event.getActionIndex()) < getIntField(thiz, "mStatusBarMinHeight");

                        if (quickPulldown) {
                            setBooleanField(thiz, "mQsExpandImmediate", true);
                            callMethod(thiz, "requestPanelHeightUpdate");
                            callMethod(thiz, "setListening", true);
                        }
                    }
                });
    }

    private boolean shouldQuickSettingsIntercept(Object o, float x, float y, float yDiff) {
        if (!getBooleanField(o, "mQsExpansionEnabled")) {
            return false;
        }

        final int w = (int) callMethod(o, "getMeasuredWidth");
        float region = (w * (1.f/4.f));
        boolean showQsOverride = (x > w - region);

        if (getBooleanField(o, "mQsExpanded")) {
            Object scrollView = getObjectField(o, "mScrollView");
            return ((boolean) callMethod(scrollView, "isScrolledToBottom") && yDiff < 0) &&
                    (boolean) callMethod(o, "isInQsArea", x, y);
        } else {
            return showQsOverride;
        }
    }

}

