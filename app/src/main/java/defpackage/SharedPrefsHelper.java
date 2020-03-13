package defpackage;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SharedPrefsHelper {
    public static final SharedPrefsEntry customizeApps = new CustomizeApps();
    public static final SharedPrefsEntry customizeCommands = new CustomizeCommands();
    public static final SharedPrefsEntry customizeKeywords = new CustomizeKeywords();
    public static final SharedPrefsEntry customizeLibNames = new CustomizeLibNames();

    public static class CustomizeApps extends SharedPrefsEntry {
        public static final Set<String> apps = new HashSet<>(Arrays.asList(SharedPrefsValues.CUSTOMIZE_APPS));

        public Set<String> value() {
            return apps;
        }

        public String fileName() {
            return "CustomizeApps";
        }

        public String key() {
            return "com.coffaceAPPS_LIST";
        }
    }

    public static class CustomizeCommands extends SharedPrefsEntry {
        public static final Set<String> commands = new HashSet<>(Arrays.asList(SharedPrefsValues.CUSTOMIZE_COMMANDS));

        public Set<String> value() {
            return commands;
        }

        public String fileName() {
            return "CustomizeCommands";
        }

        public String key() {
            return "com.coffaceAPPS_SET";
        }
    }

    public static class CustomizeKeywords extends SharedPrefsEntry {
        public static final Set<String> keywords = new HashSet<>(Arrays.asList(SharedPrefsValues.CUSTOMIZE_KEYWORDS));

        public Set<String> value() {
            return keywords;
        }

        public String fileName() {
            return "CustomizeKeywords";
        }

        public String key() {
            return "com.coffaceSET";
        }
    }

    public static class CustomizeLibNames extends SharedPrefsEntry {
        public static final Set<String> libNames = new HashSet<>(Arrays.asList(SharedPrefsValues.CUSTOMIZE_LIBNAMES));

        public Set<String> value() {
            return libNames;
        }

        public String fileName() {
            return "CustomizeLibnames";
        }

        public String key() {
            return "LIBNAMES_SET";
        }
    }

    public static abstract class SharedPrefsEntry {
        public abstract Set<String> value();

        public abstract String fileName();

        public abstract String key();
    }
}
