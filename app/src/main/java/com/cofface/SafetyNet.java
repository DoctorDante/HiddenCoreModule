package com.cofface;

import org.json.JSONObject;

import java.io.File;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class SafetyNet implements IXposedHookLoadPackage {

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if ("android".equals(loadPackageParam.packageName)) {
            spoofSelinuxAndHideRootBinary();
        }
        spoofSafetyNetResult();
    }

    private void spoofSelinuxAndHideRootBinary() {
        XposedHelpers.findAndHookMethod(File.class, "exists", new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                File file = (File) methodHookParam.thisObject;
                if (new File("/sys/fs/selinux/enforce").equals(file)) {
                    methodHookParam.setResult(true);
                } else if (new File("/system/bin/su").equals(file) || new File("/system/xbin/su").equals(file)) {
                    methodHookParam.setResult(false);
                }
            }
        });
    }

    private void spoofSafetyNetResult() {
        XposedHelpers.findAndHookMethod(JSONObject.class, "getBoolean", String.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                String key = (String) methodHookParam.args[0];
                if ("ctsProfileMatch".equals(key) || "basicIntegrity".equals(key) || "isValidSignature".equals(key)) {
                    methodHookParam.setResult(true);
                }
            }
        });
    }
}
