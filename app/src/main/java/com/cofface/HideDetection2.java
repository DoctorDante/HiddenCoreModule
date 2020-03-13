package com.cofface;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Instrumentation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.StrictMode;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.callbacks.XCallback;
import defpackage.SharedPrefsHelper;

public class HideDetection2 implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    public static final int BACK = 4;
    private static final String FAKE_APPLICATION = "FAKE.JUNK.APPLICATION";
    private static final String FAKE_COMMAND = "FAKEJUNKCOMMAND";
    private static final String FAKE_FILE = "FAKEJUNKFILE";
    private static final String FAKE_PACKAGE = "FAKE.JUNK.PACKAGE";
    public static final int LONG_PRESS_TIMEOUT = 1100;
    public static final int METHOD_LONG_PRESS = 0;
    public static final int METHOD_TEST = 2;
    public static final int METHOD_THREE_KEYS = 5;
    public static final int METHOD_TIMETEST = 3;
    public static final int METHOD_TOUCH = 4;
    public static final int METHOD_TWO_KEYS = 1;
    public static final int TYPE_APP = 1;
    public static final int TYPE_SYSTEM = 0;
    private static final String VIBRATOR_SERVICE = "com.android.server.VibratorService";
    public static final int VOL_DOWN = 25;
    public static final int VOL_UP = 24;

    private static boolean[] activeKeyPressed = new boolean[3];
    private static int[] activeKeys = new int[3];
    private static List<String> alwaysOnPackages = null;
    private static String applicationLabel = null;
    private static Activity currentActivity = null;
    private static int currentMethode = -1;
    public static final boolean debug = false;
    private static boolean flagKeepScreenOn;
    private static boolean isTouch;
    private static long lastDown;
    private static long[] lastKeyDown = new long[3];
    private static long lastUp;
    private static long lastUpdate;
    private static Context mContext;
    private static float mVibStrength;
    private static String packageName;
    private static XSharedPreferences prefs;
    private static boolean systemwideScreenOn;
    private Set<String> appSet;
    private Set<String> commandSet;
    private boolean debugPref;
    private boolean isRootCloakLoadingPref = false;
    private Set<String> keywordSet;
    private Set<String> libnameSet;
    private String mSdcard;

    private Boolean anyWordEndingWithKeyword(String keyword, String[] wordArray) {
        for (String tempString : wordArray) {
            if (tempString.endsWith(keyword)) {
                return true;
            }
        }
        return false;
    }

    public boolean stringEndsWithFromSet(String str, Set<String> set) {
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

    public boolean isFlagKeepScreenOn() {
        int flags, flag;
        flags = currentActivity.getWindow().getAttributes().flags;
        flag = flags & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        return flag != 0;
    }

    public boolean stringContainsFromSet(String str, Set<String> set) {
        if (str == null || set == null) {
            return false;
        }
        for (String entry : set) {
            if (str.matches(".*(\\W|^)" + entry + "(\\W|$).*")) {
                return true;
            }
        }
        return false;
    }

    private void showToast(String message) {
        if (currentActivity != null) {
            Toast toast = Toast.makeText(currentActivity, message, Toast.LENGTH_SHORT);
            TextView v = toast.getView().findViewById(android.R.id.message);
            if (v != null) v.setGravity(Gravity.CENTER);
            if (debug) {
                XposedBridge.log("Showing Toast: " + message);
            }
            toast.show();
        }
    }

    private String[] buildGrepArraySingle(String[] original, boolean addSh) {
        StringBuilder sb = new StringBuilder();
        ArrayList<String> arrayList = new ArrayList<>();
        if (addSh) {
            arrayList.add("sh");
            arrayList.add("-c");
        }
        for (String append : original) {
            sb.append(" ");
            sb.append(append);
        }
        for (String append2 : this.keywordSet) {
            sb.append(" | grep -v ");
            sb.append(append2);
        }
        arrayList.add(sb.toString());
        return arrayList.toArray(new String[0]);
    }

    private void hideXposedClassesDetection(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod("java.lang.Class", loadPackageParam.classLoader, "forName", String.class, Boolean.TYPE, ClassLoader.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam methodHookParam) {
                String str = (String) methodHookParam.args[0];
                if (str == null) {
                    return;
                }
                if (str.equals("XposedBridge") || str.equals("XC_MethodReplacement")) {
                    methodHookParam.setThrowable(new ClassNotFoundException());
                }
            }
        });
        XposedHelpers.findAndHookMethod("java.lang.Class", loadPackageParam.classLoader, "forName", String.class, Boolean.TYPE, ClassLoader.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam methodHookParam) {
                String str = (String) methodHookParam.args[0];
                if (str == null) {
                    return;
                }
                if (str.equals("XposedBridge") || str.equals("XC_MethodReplacement")) {
                    methodHookParam.setThrowable(new ClassNotFoundException());
                }
            }
        });
        XposedHelpers.findAndHookMethod(StringWriter.class, "toString", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                if (((String) methodHookParam.getResult()).toLowerCase().contains("de.robv.android.xposed")) {
                    methodHookParam.setResult("");
                }
            }
        });
    }

    private void spoofInstalledApps(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", loadPackageParam.classLoader, "getInstalledApplications", Integer.TYPE, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                List<ApplicationInfo> list = (List<ApplicationInfo>) methodHookParam.getResult();
                Iterator<ApplicationInfo> it = list.iterator();
                while (it.hasNext()) {
                    if (it.next().packageName != null) {
                        it.remove();
                    }
                }
                methodHookParam.setResult(list);
            }
        });
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", loadPackageParam.classLoader, "getInstalledPackages", Integer.TYPE, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                List<PackageInfo> list = (List<PackageInfo>) methodHookParam.getResult();
                Iterator<PackageInfo> it = list.iterator();
                while (it.hasNext()) {
                    if (it.next().packageName != null) {
                        it.remove();
                    }
                }
                methodHookParam.setResult(list);
            }
        });
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", loadPackageParam.classLoader, "getPackageInfo", String.class, Integer.TYPE, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam methodHookParam) {
                String str = (String) methodHookParam.args[0];
                if (str != null && stringContainsFromSet(str, keywordSet)) {
                    methodHookParam.args[0] = HideDetection2.FAKE_PACKAGE;
                }
            }
        });
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", loadPackageParam.classLoader, "getApplicationInfo", String.class, Integer.TYPE, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam methodHookParam) {
                String str = (String) methodHookParam.args[0];
                if (str != null && stringContainsFromSet(str, keywordSet)) {
                    methodHookParam.args[0] = HideDetection2.FAKE_APPLICATION;
                }
            }
        });
    }

    private void spoofGps(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod("android.location.Location", loadPackageParam.classLoader, "getAccuracy", new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                super.beforeHookedMethod(methodHookParam);
                methodHookParam.setResult(0.01f);
            }
        });
    }

    private void spoofMockLocationSettings(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod("android.provider.Settings.Secure", loadPackageParam.classLoader, "getString", ContentResolver.class, String.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam methodHookParam) {
                if (methodHookParam.args[1].equals("mock_location")) {
                    methodHookParam.setResult("0");
                }
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            XposedHelpers.findAndHookMethod("android.provider.Settings.Secure", loadPackageParam.classLoader, "getStringForUser", ContentResolver.class, String.class, Integer.TYPE, new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam methodHookParam) {
                    if (methodHookParam.args[1].equals("mock_location")) {
                        methodHookParam.setResult("0");
                    }
                }
            });
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            XposedHelpers.findAndHookMethod("android.location.Location", loadPackageParam.classLoader, "isFromMockProvider", new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam methodHookParam) {
                    methodHookParam.setResult(false);
                }
            });
        }
    }

    private void initActivityManager(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod("android.app.ActivityManager", loadPackageParam.classLoader, "getRunningServices", Integer.TYPE, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                if (debugPref) {
                    XposedBridge.log("Hooked getRunningServices");
                }
                List<ActivityManager.RunningServiceInfo> list = (List<ActivityManager.RunningServiceInfo>) methodHookParam.getResult();
                Iterator<ActivityManager.RunningServiceInfo> it = list.iterator();
                while (it.hasNext()) {
                    String str = it.next().process;
                    if (str != null && stringContainsFromSet(str, keywordSet)) {
                        it.remove();
                        if (debugPref) {
                            XposedBridge.log("Found and hid service: " + str);
                        }
                    }
                }
                methodHookParam.setResult(list);
            }
        });
    }

    private void initFile(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedBridge.hookMethod(XposedHelpers.findConstructorExact(File.class, String.class), new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {

            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (methodHookParam.args[0] != null && debugPref) {
                    XposedBridge.log("File: Found a File constructor: " + methodHookParam.args[0]);
                }
                if (!isRootCloakLoadingPref) {
                    if (((String) methodHookParam.args[0]).endsWith("su")) {
                        if (debugPref) {
                            XposedBridge.log("File: Found a File constructor ending with su");
                        }
                        methodHookParam.args[0] = "/system/xbin/FAKEJUNKFILE";
                    } else if (((String) methodHookParam.args[0]).endsWith("busybox")) {
                        if (debugPref) {
                            XposedBridge.log("File: Found a File constructor ending with busybox");
                        }
                        methodHookParam.args[0] = "/system/xbin/FAKEJUNKFILE";
                    } else if (stringContainsFromSet((String) methodHookParam.args[0], keywordSet)) {
                        if (debugPref) {
                            XposedBridge.log("File: Found a File constructor with word super, noshufou, or chainfire");
                        }
                        methodHookParam.args[0] = "/system/app/FAKEJUNKFILE.apk";
                    }
                }
            }
        });
        XposedBridge.hookMethod(XposedHelpers.findConstructorExact(File.class, String.class, String.class), new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {

            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (!(methodHookParam.args[0] == null || methodHookParam.args[1] == null || !debugPref)) {
                    XposedBridge.log("File: Found a File constructor: " + methodHookParam.args[0] + ", with: " + methodHookParam.args[1]);
                }
                if (!isRootCloakLoadingPref) {
                    if (((String) methodHookParam.args[1]).equalsIgnoreCase("su")) {
                        if (debugPref) {
                            XposedBridge.log("File: Found a File constructor with filename su");
                        }
                        methodHookParam.args[1] = FAKE_FILE;
                    } else if (((String) methodHookParam.args[1]).contains("busybox")) {
                        if (debugPref) {
                            XposedBridge.log("File: Found a File constructor ending with busybox");
                        }
                        methodHookParam.args[1] = FAKE_FILE;
                    } else if (stringContainsFromSet((String) methodHookParam.args[1], keywordSet)) {
                        if (debugPref) {
                            XposedBridge.log("File: Found a File constructor with word super, noshufou, or chainfire");
                        }
                        methodHookParam.args[1] = "FAKEJUNKFILE.apk";
                    }
                }
            }
        });
        XposedBridge.hookMethod(XposedHelpers.findConstructorExact(File.class, URI.class), new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                if (methodHookParam.args[0] != null && debugPref) {
                    XposedBridge.log("File: Found a URI File constructor: " + (methodHookParam.args[0]).toString());
                }
            }
        });
    }

    private void initOther(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod("android.os.Debug", loadPackageParam.classLoader, "isDebuggerConnected", XC_MethodReplacement.returnConstant(false));
        if (!Build.TAGS.equals("release-keys")) {
            if (this.debugPref) {
                XposedBridge.log("Original build tags: " + Build.TAGS);
            }
            XposedHelpers.setStaticObjectField(Build.class, "TAGS", "release-keys");
            if (this.debugPref) {
                XposedBridge.log("New build tags: " + Build.TAGS);
            }
        } else if (this.debugPref) {
            XposedBridge.log("No need to change build tags: " + Build.TAGS);
        }
        XposedHelpers.findAndHookMethod("android.os.SystemProperties", loadPackageParam.classLoader, "get", String.class, new XC_MethodHook() {

            protected void beforeHookedMethod(MethodHookParam methodHookParam) {
                if (methodHookParam.args[0].equals("ro.build.selinux")) {
                    methodHookParam.setResult("1");
                    if (debugPref) {
                        XposedBridge.log("SELinux is enforced.");
                    }
                }
            }
        });
        XposedHelpers.findAndHookMethod("java.lang.Class", loadPackageParam.classLoader, "forName", String.class, Boolean.TYPE, ClassLoader.class, new XC_MethodHook() {

            protected void beforeHookedMethod(MethodHookParam methodHookParam) {
                String str = (String) methodHookParam.args[0];
                if (str == null) {
                    return;
                }
                if (str.equals("XposedBridge") || str.equals("XC_MethodReplacement")) {
                    methodHookParam.setThrowable(new ClassNotFoundException());
                    if (debugPref) {
                        XposedBridge.log("Found and hid Xposed class name: " + str);
                    }
                }
            }
        });
    }

    private void initPackageManager(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", loadPackageParam.classLoader, "getInstalledApplications", Integer.TYPE, new XC_MethodHook() {

            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                if (debugPref) {
                    XposedBridge.log("Hooked getInstalledApplications");
                }
                List<ApplicationInfo> list = (List<ApplicationInfo>) methodHookParam.getResult();
                Iterator<ApplicationInfo> it = list.iterator();
                while (it.hasNext()) {
                    String str = ((ApplicationInfo) it.next()).packageName;
                    if (str != null && stringContainsFromSet(str, keywordSet)) {
                        it.remove();
                        if (debugPref) {
                            XposedBridge.log("Found and hid package: " + str);
                        }
                    }
                }
                methodHookParam.setResult(list);
            }
        });
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", loadPackageParam.classLoader, "getInstalledPackages", Integer.TYPE, new XC_MethodHook() {

            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                if (debugPref) {
                    XposedBridge.log("Hooked getInstalledPackages");
                }
                List<PackageInfo> list = (List<PackageInfo>) methodHookParam.getResult();
                Iterator<PackageInfo> it = list.iterator();
                while (it.hasNext()) {
                    String str = it.next().packageName;
                    if (str != null && stringContainsFromSet(str, keywordSet)) {
                        it.remove();
                        if (debugPref) {
                            XposedBridge.log("Found and hid package: " + str);
                        }
                    }
                }
                methodHookParam.setResult(list);
            }
        });
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", loadPackageParam.classLoader, "getPackageInfo", String.class, Integer.TYPE, new XC_MethodHook() {

            protected void beforeHookedMethod(MethodHookParam methodHookParam) {
                if (debugPref) {
                    XposedBridge.log("Hooked getPackageInfo");
                }
                String str = (String) methodHookParam.args[0];
                if (str != null && stringContainsFromSet(str, keywordSet)) {
                    methodHookParam.args[0] = HideDetection2.FAKE_PACKAGE;
                    if (debugPref) {
                        XposedBridge.log("Found and hid package: " + str);
                    }
                }
            }
        });
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", loadPackageParam.classLoader, "getApplicationInfo", String.class, Integer.TYPE, new XC_MethodHook() {

            protected void beforeHookedMethod(MethodHookParam methodHookParam) {
                String str = (String) methodHookParam.args[0];
                if (debugPref) {
                    XposedBridge.log("Hooked getApplicationInfo : " + str);
                }
                if (str != null && stringContainsFromSet(str, keywordSet)) {
                    methodHookParam.args[0] = HideDetection2.FAKE_APPLICATION;
                    if (debugPref) {
                        XposedBridge.log("Found and hid application: " + str);
                    }
                }
            }
        });
    }

    private void initProcessBuilder(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedBridge.hookMethod(XposedHelpers.findConstructorExact(ProcessBuilder.class, String[].class), new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                XposedBridge.log("Hooked ProcessBuilder");
                if (methodHookParam.args[0] != null) {
                    String[] strArr = (String[]) methodHookParam.args[0];
                    if (debugPref) {
                        String str = "ProcessBuilder Command:";
                        for (String str2 : strArr) {
                            str = str + " " + str2;
                        }
                        XposedBridge.log(str);
                    }
                    if (stringEndsWithFromSet(strArr[0], commandSet)) {
                        strArr[0] = FAKE_COMMAND;
                        methodHookParam.args[0] = strArr;
                    }
                    if (debugPref) {
                        String str3 = "New ProcessBuilder Command:";
                        for (String str4 : (String[]) methodHookParam.args[0]) {
                            str3 = str3 + " " + str4;
                        }
                        XposedBridge.log(str3);
                    }
                }
            }
        });
    }

    private void initRuntime(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod("java.lang.Runtime", loadPackageParam.classLoader, "exec", String[].class, String[].class, File.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam methodHookParam) {
                if (debugPref) {
                    XposedBridge.log("Hooked Runtime.exec");
                }
                String[] strArr = (String[]) methodHookParam.args[0];
                if (strArr != null && strArr.length >= 1) {
                    String str = strArr[0];
                    if (debugPref) {
                        String str2 = "Exec Command:";
                        for (String str3 : strArr) {
                            str2 = str2 + " " + str3;
                        }
                        XposedBridge.log(str2);
                    }
                    if (stringEndsWithFromSet(str, commandSet)) {
                        if (debugPref) {
                            XposedBridge.log("Found blacklisted command at the end of the string: " + str);
                        }
                        if (str.equals("su") || str.endsWith("/su")) {
                            methodHookParam.setThrowable(new IOException());
                        } else if (!commandSet.contains("pm") || (!str.equals("pm") && !str.endsWith("/pm"))) {
                            if (commandSet.contains("ps") && (str.equals("ps") || str.endsWith("/ps"))) {
                                methodHookParam.args[0] = buildGrepArraySingle(strArr, true);
                            } else if (commandSet.contains("which") && (str.equals("which") || str.endsWith("/which"))) {
                                methodHookParam.setThrowable(new IOException());
                            } else if (commandSet.contains("busybox") && anyWordEndingWithKeyword("busybox", strArr)) {
                                methodHookParam.setThrowable(new IOException());
                            } else if (!commandSet.contains("sh") || (!str.equals("sh") && !str.endsWith("/sh"))) {
                                methodHookParam.setThrowable(new IOException());
                            } else {
                                methodHookParam.setThrowable(new IOException());
                            }
                        } else if (strArr.length >= 3 && strArr[1].equalsIgnoreCase("list") && strArr[2].equalsIgnoreCase("packages")) {
                            methodHookParam.args[0] = buildGrepArraySingle(strArr, true);
                        } else if (strArr.length >= 3 && ((strArr[1].equalsIgnoreCase("dump") || strArr[1].equalsIgnoreCase("path")) && stringContainsFromSet(strArr[2], keywordSet))) {
                            methodHookParam.args[0] = new String[]{strArr[0], strArr[1], HideDetection2.FAKE_PACKAGE};
                        }
                        if (debugPref && methodHookParam.getThrowable() == null) {
                            String str4 = "New Exec Command:";
                            for (String str5 : (String[]) methodHookParam.args[0]) {
                                str4 = str4 + " " + str5;
                            }
                            XposedBridge.log(str4);
                        }
                    }
                } else if (debugPref) {
                    XposedBridge.log("Null or empty array on exec");
                }
            }
        });
        XposedHelpers.findAndHookMethod("java.lang.Runtime", loadPackageParam.classLoader, "loadLibrary", String.class, ClassLoader.class, new XC_MethodHook() {

            protected void beforeHookedMethod(MethodHookParam methodHookParam) {
                if (debugPref) {
                    XposedBridge.log("Hooked loadLibrary");
                }
                String str = (String) methodHookParam.args[0];
                if (str != null && stringContainsFromSet(str, libnameSet)) {
                    methodHookParam.setResult(null);
                    if (debugPref) {
                        XposedBridge.log("Loading of library " + str + " disabled.");
                    }
                }
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void initSettingsGlobal(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod(Settings.Global.class, "getInt", ContentResolver.class, String.class, Integer.TYPE, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam methodHookParam) {
                String str = (String) methodHookParam.args[1];
                if (str != null && "adb_enabled".equals(str)) {
                    methodHookParam.setResult(0);
                    if (debugPref) {
                        XposedBridge.log("Hooked ADB debugging info, adb status is off");
                    }
                }
            }
        });
    }

    private void bypassSignatureVerification() {
        ClassLoader classLoader = null;
        XposedHelpers.findAndHookMethod("java.security.MessageDigest", classLoader, "isEqual", byte[].class, byte[].class, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(Boolean.TRUE);
            }
        });
        XposedHelpers.findAndHookMethod("java.security.Signature", classLoader, "verify", byte[].class, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(Boolean.TRUE);
            }
        });
        XposedHelpers.findAndHookMethod("java.security.Signature", classLoader, "verify", byte[].class, Integer.TYPE, Integer.TYPE, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(Boolean.TRUE);
            }
        });
    }

    private static Set<String> loadSetFromPrefs(SharedPrefsHelper.SharedPrefsEntry eVar) {
        StrictMode.ThreadPolicy threadPolicy = StrictMode.getThreadPolicy();
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(threadPolicy).permitDiskReads().permitDiskWrites().build());
        HashSet<String> hashSet = new HashSet<>();
        try {
            XSharedPreferences xSharedPreferences = new XSharedPreferences("com.cofface", eVar.fileName());
            xSharedPreferences.makeWorldReadable();
            boolean z = xSharedPreferences.getBoolean("com.coffaceIS_FIRST_RUN", true);
            Set<String> stringSet = xSharedPreferences.getStringSet(eVar.key(), null);
            if (stringSet != null) {
                hashSet.addAll(stringSet);
            } else if (z) {
                hashSet.addAll(eVar.value());
            }
            return hashSet;
        } finally {
            StrictMode.setThreadPolicy(threadPolicy);
        }
    }

    private void hookRootCheckingLibraries(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod("com.kozhevin.rootchecks.util.MeatGrinder", loadPackageParam.classLoader, "isDetectedDevKeys", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("com.kozhevin.rootchecks.util.MeatGrinder", loadPackageParam.classLoader, "isDetectedTestKeys", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("com.kozhevin.rootchecks.util.MeatGrinder", loadPackageParam.classLoader, "isNotFoundReleaseKeys", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("com.kozhevin.rootchecks.util.MeatGrinder", loadPackageParam.classLoader, "isFoundDangerousProps", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("com.kozhevin.rootchecks.util.MeatGrinder", loadPackageParam.classLoader, "isPermissiveSelinux", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("com.kozhevin.rootchecks.util.MeatGrinder", loadPackageParam.classLoader, "isSuExists", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("com.kozhevin.rootchecks.util.MeatGrinder", loadPackageParam.classLoader, "isAccessedSuperuserApk", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("com.kozhevin.rootchecks.util.MeatGrinder", loadPackageParam.classLoader, "isFoundSuBinary", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("com.kozhevin.rootchecks.util.MeatGrinder", loadPackageParam.classLoader, "isFoundBusyboxBinary", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("com.kozhevin.rootchecks.util.MeatGrinder", loadPackageParam.classLoader, "isFoundXposed", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("com.kozhevin.rootchecks.util.MeatGrinder", loadPackageParam.classLoader, "isFoundResetprop", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("com.kozhevin.rootchecks.util.MeatGrinder", loadPackageParam.classLoader, "isFoundWrongPathPermission", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("com.kozhevin.rootchecks.util.MeatGrinder", loadPackageParam.classLoader, "isFoundHooks", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
    }

    private void hideXposed(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        Class<?> cls;
        XposedBridge.log("XposedHider - Protected - " + loadPackageParam.packageName);
        XC_MethodHook callback = new XC_MethodHook() {
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                String str = (String) methodHookParam.args[0];
                if (str.matches("de\\.robv\\.android\\.xposed\\.Xposed+.+")) {
                    methodHookParam.setThrowable(new ClassNotFoundException(str));
                }
            }
        };
        XposedHelpers.findAndHookMethod(ClassLoader.class, "loadClass", String.class, Boolean.TYPE, callback);
        XposedHelpers.findAndHookMethod(Class.class, "forName", String.class, Boolean.TYPE, ClassLoader.class, callback);
        XposedHelpers.findAndHookConstructor(File.class, String.class, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam methodHookParam) {
                String str = (String) methodHookParam.args[0];
                if (str.matches("/proc/[0-9]+/maps") || (str.toLowerCase().contains("xposed") && !str.startsWith(mSdcard) && !str.contains("fkzhang"))) {
                    methodHookParam.args[0] = "/system/build.prop";
                }
            }
        });
        XC_MethodHook callback2 = new XC_MethodHook() {
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                StackTraceElement[] stackTraceElementArr = (StackTraceElement[]) methodHookParam.getResult();
                ArrayList<StackTraceElement> arrayList = new ArrayList<>();
                for (StackTraceElement stackTraceElement : stackTraceElementArr) {
                    if (!stackTraceElement.getClassName().toLowerCase().contains("xposed")) {
                        arrayList.add(stackTraceElement);
                    }
                }
                methodHookParam.setResult(arrayList.toArray(new StackTraceElement[0]));
            }
        };

        XposedHelpers.findAndHookMethod(Throwable.class, "getStackTrace", callback2);
        XposedHelpers.findAndHookMethod(Thread.class, "getStackTrace", callback2);
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", loadPackageParam.classLoader, "getInstalledPackages", Integer.TYPE, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                List<PackageInfo> list = (List<PackageInfo>) methodHookParam.getResult();
                ArrayList<PackageInfo> arrayList = new ArrayList<>();
                int size = list.size();
                for (int i = 0; i < size; i++) {
                    PackageInfo packageInfo = list.get(i);
                    if (!packageInfo.packageName.toLowerCase().contains("xposed")) {
                        arrayList.add(packageInfo);
                    }
                }
                methodHookParam.setResult(arrayList);
            }
        });
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", loadPackageParam.classLoader, "getInstalledApplications", Integer.TYPE, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                List<ApplicationInfo> list = (List<ApplicationInfo>) methodHookParam.getResult();
                ArrayList<ApplicationInfo> arrayList = new ArrayList<>();
                int size = list.size();
                for (int i = 0; i < size; i++) {
                    ApplicationInfo applicationInfo = list.get(i);
                    if (!((applicationInfo.metaData != null && applicationInfo.metaData.getBoolean("xposedmodule")) || (applicationInfo.packageName != null && applicationInfo.packageName.toLowerCase().contains("xposed")) || ((applicationInfo.className != null && applicationInfo.className.toLowerCase().contains("xposed")) || (applicationInfo.processName != null && applicationInfo.processName.toLowerCase().contains("xposed"))))) {
                        arrayList.add(applicationInfo);
                    }
                }
                methodHookParam.setResult(arrayList);
            }
        });
        XposedHelpers.findAndHookMethod(Modifier.class, "isNative", Integer.TYPE, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod(System.class, "getProperty", String.class, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                if ("vxp".equals(methodHookParam.args[0])) {
                    methodHookParam.setResult(null);
                }
            }
        });
        XposedHelpers.findAndHookMethod(File.class, "list", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                String[] strArr = (String[]) methodHookParam.getResult();
                if (strArr != null) {
                    ArrayList<String> arrayList = new ArrayList<>();
                    for (String str : strArr) {
                        if (!str.toLowerCase().contains("xposed") && !str.equals("su")) {
                            arrayList.add(str);
                        }
                    }
                    methodHookParam.setResult(arrayList.toArray(new String[0]));
                }
            }
        });
        try {
            cls = Runtime.getRuntime().exec("echo").getClass();
        } catch (IOException unused) {
            XposedBridge.log("[W/XposedHider] Cannot hook Process#getInputStream");
            cls = null;
        }
        if (cls != null) {
            XposedHelpers.findAndHookMethod(cls, "getInputStream", new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam methodHookParam) {
                    methodHookParam.setResult(methodHookParam.getResult());
                }
            });
            XposedBridge.hookAllMethods(System.class, "getenv", new XC_MethodHook() {
                private String a(String str) {
                    List<String> asList = Arrays.asList(str.split(":"));
                    ArrayList<String> arrayList = new ArrayList<>();
                    for (int i = 0; i < asList.size(); i++) {
                        if (!asList.get(i).toLowerCase().contains("xposed")) {
                            arrayList.add(asList.get(i));
                        }
                    }
                    StringBuilder sb = new StringBuilder();
                    for (int i2 = 0; i2 < arrayList.size(); i2++) {
                        sb.append(arrayList);
                        if (i2 != arrayList.size() - 1) {
                            sb.append(":");
                        }
                    }
                    return sb.toString();
                }

                protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    if (methodHookParam.args.length == 0) {
                        methodHookParam.setResult(a((String) ((Map) methodHookParam.getResult()).get("CLASSPATH")));
                    } else if ("CLASSPATH".equals(methodHookParam.args[0])) {
                        methodHookParam.setResult(a((String) methodHookParam.getResult()));
                    }
                }
            });
        }
    }

    private void readPrefs() {
        prefs = new XSharedPreferences("com.cofface.invader", "gps");
        prefs.reload();
        LinkedList<String> linkedList = new LinkedList<>();
        if (prefs.getBoolean("goj", true)) {
            linkedList.add("com.gojek.driver.bike");
        }
        if (prefs.getBoolean("goc", true)) {
            linkedList.add("com.gojek.driver.car");
        }
        alwaysOnPackages = linkedList;
        systemwideScreenOn = prefs.getBoolean("systemwide", true);
        lastUpdate = System.currentTimeMillis();
    }

    private void rootbeer(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod("com.scottyab.rootbeer.RootBeer", loadPackageParam.classLoader, "detectRootManagementApps", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("com.scottyab.rootbeer.RootBeer", loadPackageParam.classLoader, "detectPotentiallyDangerousApps", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("com.scottyab.rootbeer.RootBeer", loadPackageParam.classLoader, "detectTestKeys", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("com.scottyab.rootbeer.RootBeer", loadPackageParam.classLoader, "checkForBusyBoxBinary", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("com.scottyab.rootbeer.RootBeer", loadPackageParam.classLoader, "checkForSuBinary", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("com.scottyab.rootbeer.RootBeer", loadPackageParam.classLoader, "checkSuExists", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("com.scottyab.rootbeer.RootBeer", loadPackageParam.classLoader, "checkForRWPaths", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("com.scottyab.rootbeer.RootBeer", loadPackageParam.classLoader, "checkForDangerousProps", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("com.scottyab.rootbeer.RootBeer", loadPackageParam.classLoader, "checkForRootNative", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("com.scottyab.rootbeer.RootBeer", loadPackageParam.classLoader, "detectRootCloakingApps", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("com.scottyab.rootbeer.RootBeer", loadPackageParam.classLoader, "isSelinuxFlagInEnabled", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("com.scottyab.rootbeer.RootBeer", loadPackageParam.classLoader, "isRooted", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("com.scottyab.rootbeer.RootBeer", loadPackageParam.classLoader, "isRootedWithoutBusyBoxCheck", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("com.scottyab.rootbeer.RootBeer", loadPackageParam.classLoader, "canLoadNativeLibrary", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("com.scottyab.rootbeer.RootBeerNative", loadPackageParam.classLoader, "wasNativeLibraryLoaded", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
    }

    private void setApplicationLabel() {
        PackageManager pm;
        ApplicationInfo ai;
        packageName = currentActivity.getPackageName();
        try {
            pm = currentActivity.getPackageManager();
            ai = pm.getApplicationInfo(currentActivity.getPackageName(), 0);
            applicationLabel = (String) (ai != null ? pm.getApplicationLabel(ai) : packageName);
        } catch (PackageManager.NameNotFoundException | NullPointerException e) {
            XposedBridge.log(e);
            applicationLabel = packageName;
        }
        if (applicationLabel == null) {
            applicationLabel = "App";
        }
    }

    private boolean setFlagKeepScreenOn(boolean keepScreenOn, int type) {
        if (type == TYPE_SYSTEM) {
            systemwideScreenOn = flagKeepScreenOn = keepScreenOn;
            if (keepScreenOn) {
                currentActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                currentActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        } else if (type == TYPE_APP) {
            flagKeepScreenOn = keepScreenOn;
            if (keepScreenOn) {
                currentActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                currentActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
            if (flagKeepScreenOn) {

            }
        } else {
            // should not happen
        }
        return isFlagKeepScreenOn();
    }

    //What's cku???
    private void spoofGps2(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XposedHelpers.findAndHookMethod(LocationManager.class, "getLastLocation", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                Location location = new Location("network");
                //location.setLatitude(Double.valueOf(cku.a().a).doubleValue());
                //location.setLongitude(Double.valueOf(cku.a().b).doubleValue());
                location.setAccuracy(0.01f);
                location.setTime(System.currentTimeMillis());
                if (Build.VERSION.SDK_INT >= 17) {
                    location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                }
                methodHookParam.setResult(location);
            }
        });
        XposedHelpers.findAndHookMethod(LocationManager.class, "getLastKnownLocation", String.class, new XC_MethodHook() {

            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                Location location = new Location("network");
                //location.setLatitude(Double.valueOf(cku.a().a).doubleValue());
                //location.setLongitude(Double.valueOf(cku.a().b).doubleValue());
                location.setAccuracy(0.01f);
                location.setTime(System.currentTimeMillis());
                if (Build.VERSION.SDK_INT >= 17) {
                    location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                }
                methodHookParam.setResult(location);
            }
        });
        XposedBridge.hookAllMethods(LocationManager.class, "getProviders", new XC_MethodHook() {

            protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                ArrayList<String> arrayList = new ArrayList<>();
                arrayList.add("gps");
                methodHookParam.setResult(arrayList);
            }
        });
        XposedHelpers.findAndHookMethod(LocationManager.class, "getBestProvider", Criteria.class, Boolean.TYPE, new XC_MethodHook() {
            /* class AnonymousClass24 */

            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult("gps");
            }
        });
        XposedHelpers.findAndHookMethod(LocationManager.class, "addGpsStatusListener", GpsStatus.Listener.class, new XC_MethodHook() {
            /* class AnonymousClass25 */

            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                if (methodHookParam.args[0] != null) {
                    XposedHelpers.callMethod(methodHookParam.args[0], "onGpsStatusChanged", 1);
                    XposedHelpers.callMethod(methodHookParam.args[0], "onGpsStatusChanged", 3);
                }
            }
        });
        XposedHelpers.findAndHookMethod(LocationManager.class, "addNmeaListener", GpsStatus.NmeaListener.class, new XC_MethodHook() {
            /* class AnonymousClass26 */

            protected void beforeHookedMethod(MethodHookParam methodHookParam) {
                methodHookParam.setResult(false);
            }
        });
        XposedHelpers.findAndHookMethod("android.location.LocationManager", loadPackageParam.classLoader, "getGpsStatus", GpsStatus.class, new XC_MethodHook() {

            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                GpsStatus gpsStatus = (GpsStatus) methodHookParam.getResult();
                if (gpsStatus != null) {
                    Method method = null;
                    Method[] declaredMethods = GpsStatus.class.getDeclaredMethods();
                    int length = declaredMethods.length;
                    int i = 0;
                    while (true) {
                        if (i >= length) {
                            break;
                        }
                        Method method2 = declaredMethods[i];
                        if (method2.getName().equals("setStatus") && method2.getParameterTypes().length > 1) {
                            method = method2;
                            break;
                        }
                        i++;
                    }
                    if (method != null) {
                        method.setAccessible(true);
                        int[] iArr = {1, 2, 3, 4, 5};
                        float[] fArr = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
                        float[] fArr2 = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
                        float[] fArr3 = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
                        XposedHelpers.callMethod(gpsStatus, "setStatus", 5, iArr, fArr, fArr2, fArr3, 31, 31, 31);
                        methodHookParam.args[0] = gpsStatus;
                        methodHookParam.setResult(gpsStatus);
                        try {
                            method.invoke(gpsStatus, 5, iArr, fArr, fArr2, fArr3, 31, 31, 31);
                            methodHookParam.setResult(gpsStatus);
                        } catch (Exception e) {
                            XposedBridge.log(e);
                        }
                    }
                }
            }
        });
        for (Method method : LocationManager.class.getDeclaredMethods()) {
            if (method.getName().equals("requestLocationUpdates") && !Modifier.isAbstract(method.getModifiers()) && Modifier.isPublic(method.getModifiers())) {
                XposedBridge.hookMethod(method, new XC_MethodHook() {

                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        if (methodHookParam.args.length >= 4 && (methodHookParam.args[3] instanceof LocationListener)) {
                            LocationListener locationListener = (LocationListener) methodHookParam.args[3];
                            Method method = null;
                            Method[] declaredMethods = LocationListener.class.getDeclaredMethods();
                            int length = declaredMethods.length;
                            int i = 0;
                            while (true) {
                                if (i >= length) {
                                    break;
                                }
                                Method method2 = declaredMethods[i];
                                if (method2.getName().equals("onLocationChanged") && !Modifier.isAbstract(method2.getModifiers())) {
                                    method = method2;
                                    break;
                                }
                                i++;
                            }
                            Location location = new Location("network");
                            //location.setLatitude(java.lang.Double.valueOf(cku.a().a).doubleValue());
                            //location.setLongitude(java.lang.Double.valueOf(cku.a().b).doubleValue());
                            location.setAccuracy(0.01f);
                            location.setTime(System.currentTimeMillis());
                            if (Build.VERSION.SDK_INT >= 17) {
                                location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                            }
                            XposedHelpers.callMethod(locationListener, "onLocationChanged", location);
                            if (method != null) {
                                try {
                                    method.invoke(locationListener, location);
                                } catch (Exception e) {
                                    XposedBridge.log(e);
                                }
                            }
                        }
                    }
                });
            }
            if (method.getName().equals("requestSingleUpdate ") && !Modifier.isAbstract(method.getModifiers()) && Modifier.isPublic(method.getModifiers())) {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    /* class AnonymousClass29 */

                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        if (methodHookParam.args.length >= 3 && (methodHookParam.args[1] instanceof LocationListener)) {
                            LocationListener locationListener = (LocationListener) methodHookParam.args[3];
                            Method method = null;
                            Method[] declaredMethods = LocationListener.class.getDeclaredMethods();
                            int length = declaredMethods.length;
                            int i = 0;
                            while (true) {
                                if (i >= length) {
                                    break;
                                }
                                Method method2 = declaredMethods[i];
                                if (method2.getName().equals("onLocationChanged") && !Modifier.isAbstract(method2.getModifiers())) {
                                    method = method2;
                                    break;
                                }
                                i++;
                            }
                            if (method != null) {
                                try {
                                    Location location = new Location("network");
                                    //location.setLatitude(java.lang.Double.valueOf(cku.a().a).doubleValue());
                                    //location.setLongitude(java.lang.Double.valueOf(cku.a().b).doubleValue());
                                    location.setAccuracy(0.01f);
                                    location.setTime(System.currentTimeMillis());
                                    if (Build.VERSION.SDK_INT >= 17) {
                                        location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                                    }
                                    method.invoke(locationListener, location);
                                } catch (Exception e) {
                                    XposedBridge.log(e);
                                }
                            }
                        }
                    }
                });
            }
        }
    }

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        Set<String> loadSetFromPrefs = loadSetFromPrefs(SharedPrefsHelper.customizeApps);
        prefs = new XSharedPreferences("com.cofface.invader", "com.cofface.invader");
        prefs.makeWorldReadable();
        prefs.reload();
        if (loadPackageParam.packageName.equals("com.kozhevin.rootchecks")) {
            hookRootCheckingLibraries(loadPackageParam);
        }
        if (loadPackageParam.packageName.equals("com.scottyab.rootbeer.sample")) {
            rootbeer(loadPackageParam);
        }
        if (loadSetFromPrefs.contains(loadPackageParam.packageName)) {
            this.appSet = loadSetFromPrefs;
            this.keywordSet = loadSetFromPrefs(SharedPrefsHelper.customizeKeywords);
            this.commandSet = loadSetFromPrefs(SharedPrefsHelper.customizeCommands);
            this.libnameSet = loadSetFromPrefs(SharedPrefsHelper.customizeLibNames);
            if (prefs.getBoolean("as", true)) {
                spoofMockLocationSettings(loadPackageParam);
            }
            if (prefs.getBoolean("ab", true)) {
                spoofInstalledApps(loadPackageParam);
            }
            if (prefs.getBoolean("ac", true)) {
                bypassSignatureVerification();
                hideXposed(loadPackageParam);
                hideXposedClassesDetection(loadPackageParam);
            }
            if (prefs.getBoolean("ad", true)) {
            }
            if (prefs.getBoolean("ab", true)) {
                spoofGps(loadPackageParam);
            }
            XposedHelpers.findAndHookMethod("android.os.SystemProperties", loadPackageParam.classLoader, "get", String.class, new XC_MethodHook() {
                /* class AnonymousClass59 */

                protected void beforeHookedMethod(MethodHookParam methodHookParam) {
                    if (methodHookParam.args[0].equals("ro.build.selinux")) {
                        methodHookParam.setResult("1");
                        if (debugPref) {
                            XposedBridge.log("SELinux is enforced.");
                        }
                    }
                }
            });
        }
    }

    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
        XposedHelpers.findAndHookMethod(Instrumentation.class, "newActivity", ClassLoader.class, String.class, Intent.class, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                currentActivity = (Activity) methodHookParam.getResult();
                systemwideScreenOn = false;
                flagKeepScreenOn = false;
                currentMethode = 1;
                readPrefs();
            }
        });
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {

            protected void beforeHookedMethod(MethodHookParam methodHookParam) {
                readPrefs();
                setApplicationLabel();
                if (systemwideScreenOn) {
                    boolean unused = setFlagKeepScreenOn(systemwideScreenOn, 0);
                } else if (alwaysOnPackages.contains(packageName)) {
                    String str = "Get - Screen - " + applicationLabel + "AON";
                    XposedBridge.log(str);
                    showToast(str);
                    boolean unused2 = setFlagKeepScreenOn(true, 1);
                } else if (systemwideScreenOn) {
                    boolean unused3 = setFlagKeepScreenOn(true, 1);
                }
            }
        });
        XposedHelpers.findAndHookMethod(Activity.class, "onKeyDown", Integer.TYPE, KeyEvent.class, new XC_MethodHook() {

            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                if (!(methodHookParam.args[0] instanceof Integer)) {
                    methodHookParam.setResult(false);
                }
                int intValue = (Integer) methodHookParam.args[0];
                KeyEvent keyEvent = (KeyEvent) methodHookParam.args[1];
                if (currentMethode == 0) {
                    for (int i = 0; i <= 2; i++) {
                        if (intValue == activeKeys[i]) {
                            lastKeyDown[i] = System.currentTimeMillis();
                            keyEvent.startTracking();
                        }
                    }
                } else if (currentMethode == 1) {
                    if (intValue == activeKeys[1]) {
                        if (activeKeyPressed[2]) {
                            activeKeyPressed[2] = false;
                            boolean unused = setFlagKeepScreenOn(!flagKeepScreenOn, 1);
                            methodHookParam.setResult(true);
                            return;
                        }
                        activeKeyPressed[1] = true;
                        methodHookParam.setResult(false);
                    } else if (intValue != activeKeys[2]) {
                    } else {
                        if (activeKeyPressed[1]) {
                            activeKeyPressed[1] = false;
                            boolean unused2 = setFlagKeepScreenOn(!flagKeepScreenOn, 1);
                            methodHookParam.setResult(true);
                            return;
                        }
                        activeKeyPressed[2] = true;
                        methodHookParam.setResult(false);
                    }
                } else if (currentMethode == 5) {
                    if (intValue == activeKeys[0]) {
                        if (activeKeyPressed[1]) {
                            activeKeyPressed[1] = false;
                            boolean unused3 = setFlagKeepScreenOn(!flagKeepScreenOn, 1);
                            methodHookParam.setResult(true);
                        } else if (activeKeyPressed[2]) {
                            activeKeyPressed[2] = false;
                            boolean unused4 = setFlagKeepScreenOn(!systemwideScreenOn, 0);
                            methodHookParam.setResult(true);
                        } else {
                            activeKeyPressed[0] = true;
                            methodHookParam.setResult(false);
                        }
                    } else if (intValue == activeKeys[1]) {
                        if (activeKeyPressed[0]) {
                            activeKeyPressed[0] = false;
                            boolean unused5 = setFlagKeepScreenOn(!flagKeepScreenOn, 1);
                            methodHookParam.setResult(true);
                            return;
                        }
                        activeKeyPressed[1] = true;
                        methodHookParam.setResult(false);
                    } else if (intValue != activeKeys[2]) {
                    } else {
                        if (activeKeyPressed[0]) {
                            activeKeyPressed[0] = false;
                            boolean unused6 = setFlagKeepScreenOn(!systemwideScreenOn, 0);
                            methodHookParam.setResult(true);
                            return;
                        }
                        activeKeyPressed[2] = true;
                        methodHookParam.setResult(false);
                    }
                } else if (currentMethode == 4) {
                    if (intValue == activeKeys[0]) {
                        activeKeyPressed[0] = true;
                        if (isTouch) {
                            methodHookParam.setResult(true);
                        }
                    } else if (intValue == activeKeys[1]) {
                        activeKeyPressed[1] = true;
                        if (isTouch) {
                            methodHookParam.setResult(true);
                        }
                    } else if (intValue == activeKeys[2]) {
                        activeKeyPressed[2] = true;
                        if (isTouch) {
                            methodHookParam.setResult(true);
                        }
                    }
                } else if (currentMethode == 2) {
                    Toast.makeText(currentActivity, "test", Toast.LENGTH_SHORT).show();
                    if (intValue != activeKeys[0]) {
                        if (intValue == activeKeys[1] && isTouch) {
                            boolean unused7 = setFlagKeepScreenOn(!flagKeepScreenOn, 1);
                            methodHookParam.setResult(true);
                        } else if (intValue == activeKeys[2] && isTouch) {
                            boolean unused8 = setFlagKeepScreenOn(!systemwideScreenOn, 0);
                            methodHookParam.setResult(true);
                        }
                    }
                } else if (currentMethode == 3) {
                    long j = 0;
                    long currentTimeMillis = System.currentTimeMillis();
                    if (intValue != activeKeys[0]) {
                        if (intValue == activeKeys[1]) {
                            j = currentTimeMillis - lastUp;
                            long unused9 = lastDown = currentTimeMillis;
                        } else if (intValue == activeKeys[2]) {
                            j = currentTimeMillis - lastDown;
                            long unused10 = lastUp = currentTimeMillis;
                        }
                    }
                    if (j < 300) {
                        Activity access$300 = currentActivity;
                        Toast.makeText(access$300, "distance: " + j, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        XposedHelpers.findAndHookMethod(Activity.class, "onKeyUp", Integer.TYPE, KeyEvent.class, new XC_MethodHook() {

            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                if (!(methodHookParam.args[0] instanceof Integer)) {
                    methodHookParam.setResult(false);
                }
                int intValue = (Integer) methodHookParam.args[0];
                if (currentMethode == 0) {
                    if (intValue == activeKeys[0] && lastKeyDown[0] != -1 && System.currentTimeMillis() - lastKeyDown[0] > 1100) {
                        return;
                    }
                    if (intValue == activeKeys[1] && lastKeyDown[1] != -1 && System.currentTimeMillis() - lastKeyDown[1] > 1100) {
                        lastKeyDown[1] = -1;
                        Toast.makeText(currentActivity, "woopwoop long key press", Toast.LENGTH_SHORT).show();
                    } else if (intValue == activeKeys[2] && lastKeyDown[2] != -1 && System.currentTimeMillis() - lastKeyDown[2] > 1100) {
                        lastKeyDown[1] = -1;
                        Toast.makeText(currentActivity, "woopwoop long key press", Toast.LENGTH_SHORT).show();
                    }
                } else if (currentMethode == 1 || currentMethode == 4 || currentMethode == 5) {
                    for (int i = 0; i <= 2; i++) {
                        if (intValue == activeKeys[i]) {
                            activeKeyPressed[i] = false;
                            methodHookParam.setResult(true);
                        }
                    }
                } else {
                    int unused = currentMethode;
                }
            }
        });
    }

}
