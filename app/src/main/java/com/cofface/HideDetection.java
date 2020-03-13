package com.cofface;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.Instrumentation;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.view.WindowManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.callbacks.XCallback;

public class HideDetection implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    public static final int OP_MOCK_LOCATION = 58; // AppOpsManager.OP_MOCK_LOCATION

    private static Activity currentActivity;
    private HookCallback opHook;
    private HookCallback finishOpHook;
    private Set<String> commands;
    private Set<String> keywords;
    private Set<String> libNames;
    private Set<String> appsToHook;
    private Set<String> screenAlwaysOnApps;

    private Boolean anyWordEndingWithKeyword(String keyword, String[] wordArray) {
        for (String tempString : wordArray) {
            if (tempString.endsWith(keyword)) {
                return true;
            }
        }
        return false;
    }

    public boolean stringContainsFromSet(String base, Set<String> values) {
        if (base != null && values != null) {
            for (String tempString : values) {
                if (base.matches(".*(\\W|^)" + tempString + "(\\W|$).*")) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean stringEndsWithFromSet(String str, Set<String> set) {
        if (str == null || set == null) {
            return false;
        }
        for (String entry : set) {
            if (str.endsWith(entry)) {
                return true;
            }
        }
        return false;
    }

    private String[] buildGrepArraySingle(String[] original, boolean addOnEventParameters) {
        StringBuilder builder = new StringBuilder();
        ArrayList<String> originalList = new ArrayList<String>();
        if (addOnEventParameters) {
            originalList.add("sh");
            originalList.add("-onEventParameters");
        }
        for (String temp : original) {
            builder.append(" ");
            builder.append(temp);
        }
        for (String temp : keywords) {
            builder.append(" | grep -v ");
            builder.append(temp);
        }
        originalList.add(builder.toString());
        return originalList.toArray(new String[0]);
    }

    private void hookApp(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        spoofMockLocationSettings(loadPackageParam);
        spoofInstalledApps(loadPackageParam);
        hideXposedClassesDetection(loadPackageParam);
        hideRootFilesDetection(loadPackageParam);
        hideRootExecutablesAndAppsDetection(loadPackageParam);
        XposedBridge.log("HiddenCore Module Actived");
        XposedBridge.log("HiddenCore Module Working");
        XposedBridge.log("Safety Net Enabled");
        XposedBridge.log("Hide Root - Hide Xposed - Hide Mock Location Work");
    }

    private void bypassSignatureVerification() {
        ClassLoader classLoader = null;

        XposedHelpers.findAndHookMethod("java.security.MessageDigest", classLoader, "isEqual", byte[].class, byte[].class, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                methodHookParam.setResult(Boolean.TRUE);
            }
        });

        XposedHelpers.findAndHookMethod("java.security.Signature", classLoader, "verify", byte[].class, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                methodHookParam.setResult(Boolean.TRUE);
            }
        });

        XposedHelpers.findAndHookMethod("java.security.Signature", classLoader, "verify", byte[].class, Integer.TYPE, Integer.TYPE, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                methodHookParam.setResult(Boolean.TRUE);
            }
        });
    }

    private void hookAppOpsManager(XC_LoadPackage.LoadPackageParam lpparam) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            XposedHelpers.findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "checkOp",
                    String.class, int.class, String.class, opHook);
            XposedHelpers.findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "checkOp",
                    int.class, int.class, String.class, opHook);
            XposedHelpers.findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "checkOpNoThrow",
                    String.class, int.class, String.class, opHook);
            XposedHelpers.findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "checkOpNoThrow",
                    int.class, int.class, String.class, opHook);
            XposedHelpers.findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "noteOp",
                    String.class, int.class, String.class, opHook);
            XposedHelpers.findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "noteOp",
                    int.class, int.class, String.class, opHook);
            XposedHelpers.findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "noteOpNoThrow",
                    String.class, int.class, String.class, opHook);
            XposedHelpers.findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "noteOpNoThrow",
                    int.class, int.class, String.class, opHook);
            XposedHelpers.findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "noteProxyOp",
                    String.class, String.class, opHook);
            XposedHelpers.findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "noteProxyOp",
                    int.class, String.class, opHook);
            XposedHelpers.findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "noteProxyOpNoThrow",
                    String.class, String.class, opHook);
            XposedHelpers.findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "noteProxyOpNoThrow",
                    int.class, String.class, opHook);
            XposedHelpers.findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "startOp",
                    String.class, int.class, String.class, opHook);
            XposedHelpers.findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "startOp",
                    int.class, int.class, String.class, opHook);
            XposedHelpers.findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "startOpNoThrow",
                    String.class, int.class, String.class, opHook);
            XposedHelpers.findAndHookMethod("android.app.AppOpsManager", lpparam.classLoader, "startOpNoThrow",
                    int.class, int.class, String.class, opHook);
        }
    }

    private void spoofMockLocationSettings(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod("android.provider.Settings.Secure", loadPackageParam.classLoader, "getString", ContentResolver.class, String.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                if (methodHookParam.args[1].equals("mockmock_location")) {
                    methodHookParam.setResult("0");
                }
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            XposedHelpers.findAndHookMethod("android.provider.Settings.Secure", loadPackageParam.classLoader, "getStringForUser", ContentResolver.class, String.class, Integer.TYPE, new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    if (methodHookParam.args[1].equals("mock_location")) {
                        methodHookParam.setResult("0");
                    }
                }
            });
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            XposedHelpers.findAndHookMethod("android.location.Location", loadPackageParam.classLoader, "isFromMockProvider", new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    methodHookParam.setResult(false);
                }
            });
        }
    }

    private void spoofInstalledApps(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", loadPackageParam.classLoader, "getInstalledApplications", Integer.TYPE, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                List<ApplicationInfo> list = (List<ApplicationInfo>) methodHookParam.getResult();
                Iterator<ApplicationInfo> iterator = list.iterator();
                while (iterator.hasNext()) {
                    if ((iterator.next()).packageName != null) {
                        iterator.remove();
                    }
                }
                methodHookParam.setResult(list);
            }
        });

        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", loadPackageParam.classLoader, "getInstalledPackages", Integer.TYPE, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                List<PackageInfo> list = (List<PackageInfo>) methodHookParam.getResult();
                Iterator<PackageInfo> it = list.iterator();
                while (it.hasNext()) {
                    if ((it.next()).packageName != null) {
                        it.remove();
                    }
                }
                methodHookParam.setResult(list);
            }
        });
    }

    private void hideXposedClassesDetection(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod("java.lang.Class", loadPackageParam.classLoader, "forName", String.class, Boolean.TYPE, ClassLoader.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                String str = (String) methodHookParam.args[0];
                if (str != null) {
                    if (str.equals("de.robv.android.xposed.XposedBridge") || str.equals("de.robv.android.xposed.XC_MethodReplacement")) {
                        methodHookParam.setThrowable(new ClassNotFoundException());
                    }
                }
            }
        });
    }

    private void hideRootExecutablesAndAppsDetection(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod("java.lang.Runtime", loadPackageParam.classLoader, "exec", String[].class, String[].class, File.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                String[] cmdArray = (String[]) methodHookParam.args[0];
                if (cmdArray != null && cmdArray.length >= 1) {
                    String cmdName = cmdArray[0];
                    if (stringEndsWithFromSet(cmdName, commands)) {
                        if (cmdName.equals("su") || cmdName.endsWith("/su")) {
                            methodHookParam.setThrowable(new IOException());
                        }
                        else if (!commands.contains("pm") || (!cmdName.equals("pm") && !cmdName.endsWith("/pm"))) {
                            if (commands.contains("ps") && (cmdName.equals("ps") || cmdName.endsWith("/ps"))) {
                                methodHookParam.args[0] = buildGrepArraySingle(cmdArray, true);
                            }
                            else if (commands.contains("which") && (cmdName.equals("which") || cmdName.endsWith("/which"))) {
                                methodHookParam.setThrowable(new IOException());
                            }
                            else if (commands.contains("busybox") && anyWordEndingWithKeyword("busybox", cmdArray)) {
                                methodHookParam.setThrowable(new IOException());
                            }
                            else if (!commands.contains("sh") || (!cmdName.equals("sh") && !cmdName.endsWith("/sh"))) {
                                methodHookParam.setThrowable(new IOException());
                            }
                            else {
                                methodHookParam.setThrowable(new IOException());
                            }
                        }
                        else if (cmdArray.length >= 3 && cmdArray[1].equalsIgnoreCase("list") && cmdArray[2].equalsIgnoreCase("packages")) {
                            methodHookParam.args[0] = buildGrepArraySingle(cmdArray, true);
                        }
                        else if (cmdArray.length >= 3) {
                            if ((cmdArray[1].equalsIgnoreCase("dump") || cmdArray[1].equalsIgnoreCase("path")) && stringContainsFromSet(cmdArray[2], keywords)) {
                                methodHookParam.args[0] = new String[]{cmdArray[0], cmdArray[1], "FAKE.JUNK.PACKAGE"};
                            }
                        }
                    }
                }
            }
        });


        XposedHelpers.findAndHookMethod("java.lang.Runtime", loadPackageParam.classLoader, "loadLibrary", String.class, ClassLoader.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                String str = (String) methodHookParam.args[0];
                if (str != null && stringContainsFromSet(str, libNames)) {
                    methodHookParam.setResult(null);
                }
            }
        });
    }

    private void hideRootFilesDetection(XC_LoadPackage.LoadPackageParam loadPackageParam) {

        XposedBridge.hookMethod(XposedHelpers.findConstructorExact(File.class, String.class), new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable {
                if (((String) methodHookParam.args[0]).endsWith("su")) {
                    methodHookParam.args[0] = "/system/xbin/FAKEJUNKFILE";
                }
                else if (((String) methodHookParam.args[0]).endsWith("busybox")) {
                    methodHookParam.args[0] = "/system/xbin/FAKEJUNKFILE";
                }
                else if (stringContainsFromSet((String) methodHookParam.args[0], keywords)) {
                    methodHookParam.args[0] = "/system/app/FAKEJUNKFILE.apk";
                }
            }
        });

        XposedBridge.hookMethod(XposedHelpers.findConstructorExact(File.class, String.class, String.class), new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable {
                if (((String) methodHookParam.args[1]).equalsIgnoreCase("su")) {
                    methodHookParam.args[1] = "FAKEJUNKFILE";
                }
                else if (((String) methodHookParam.args[1]).contains("busybox")) {
                    methodHookParam.args[1] = "FAKEJUNKFILE";
                }
                else if (stringContainsFromSet((String) methodHookParam.args[1], keywords)) {
                    methodHookParam.args[1] = "FAKEJUNKFILE.apk";
                }
            }
        });
    }

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        appsToHook = new HashSet<>(Arrays.asList(ModuleConfiguration.APPS_TO_HOOK));
        commands = new HashSet<>(Arrays.asList(ModuleConfiguration.COMMANDS));
        keywords = new HashSet<>(Arrays.asList(ModuleConfiguration.KEYWORDS));
        libNames = new HashSet<>(Arrays.asList(ModuleConfiguration.LIBNAMES));

        hookAppOpsManager(loadPackageParam);
        if (appsToHook.contains(loadPackageParam.packageName) || appsToHook.contains(loadPackageParam.processName)) {
            hookApp(loadPackageParam);
        }
    }

    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        XposedHelpers.findAndHookMethod(Instrumentation.class, "newActivity", ClassLoader.class, String.class, Intent.class, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                currentActivity = (Activity) methodHookParam.getResult();
            }
        });

        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                screenAlwaysOnApps = new HashSet<>(Arrays.asList(ModuleConfiguration.SCREEN_ALWAYS_ON_APPS));
                if (currentActivity != null && screenAlwaysOnApps.contains(currentActivity.getPackageName())) {
                    currentActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            opHook = new HookCallback() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Object o = param.args[0];
                    if (o.equals(OP_MOCK_LOCATION) || o.equals(AppOpsManager.OPSTR_MOCK_LOCATION)) {
                        param.setResult(AppOpsManager.MODE_ALLOWED);
                    }
                }
            };

            finishOpHook = new HookCallback() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Object o = param.args[0];
                    if (o.equals(OP_MOCK_LOCATION) || o.equals(AppOpsManager.OPSTR_MOCK_LOCATION)) {
                        param.setResult(null);
                    }
                }
            };
        }

        bypassSignatureVerification();
    }
}
