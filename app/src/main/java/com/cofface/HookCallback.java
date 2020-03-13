package com.cofface;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;

public class HookCallback extends XC_MethodHook {
    Set<String> appsToHook = new HashSet<>(Arrays.asList(ModuleConfiguration.APPS_TO_HOOK));
}
