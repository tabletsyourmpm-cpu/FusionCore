package dev.allofus.fusioncore.tools;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/**
 * Advanced logging for FusionCore.
 *
 * Every message is mirrored to logcat AND appended to a timestamped log file at
 * {@code /sdcard/FusionCore/logs/fusion_yyyyMMdd_HHmmss.txt}, so launch diagnostics
 * survive even when the process dies without going through the crash handlers.
 * Only the 10 most recent log files are kept.
 *
 * Safe to call before storage permission is granted: file logging is skipped and
 * messages still go to logcat.
 */
public final class FusionLogger {

    private static final String TAG = "FusionCore";
    private static final int MAX_KEPT_LOGS = 10;
    private static final Object LOCK = new Object();

    private static File currentLogFile;
    private static boolean fileLoggingAvailable = true;

    private FusionLogger() {
    }

    public static void i(String tag, String message) {
        log("I", tag, message, null);
    }

    public static void w(String tag, String message) {
        log("W", tag, message, null);
    }

    public static void e(String tag, String message) {
        log("E", tag, message, null);
    }

    public static void e(String tag, String message, Throwable throwable) {
        log("E", tag, message, throwable);
    }

    private static void log(String level, String tag, String message, Throwable throwable) {
        String fullTag = TAG + "/" + tag;
        switch (level) {
            case "W":
                if (throwable != null) Log.w(fullTag, message, throwable);
                else Log.w(fullTag, message);
                break;
            case "E":
                if (throwable != null) Log.e(fullTag, message, throwable);
                else Log.e(fullTag, message);
                break;
            default:
                if (throwable != null) Log.i(fullTag, message, throwable);
                else Log.i(fullTag, message);
                break;
        }
        writeToFile(level, tag, message, throwable);
    }

    private static void writeToFile(String level, String tag, String message, Throwable throwable) {
        synchronized (LOCK) {
            if (!fileLoggingAvailable) {
                return;
            }
            if (currentLogFile == null) {
                currentLogFile = createLogFile();
                if (currentLogFile == null) {
                    fileLoggingAvailable = false;
                    return;
                }
                pruneOldLogs(currentLogFile.getParentFile());
            }
            String timestamp = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date());
            try (FileWriter writer = new FileWriter(currentLogFile, true)) {
                writer.write(timestamp + " " + level + "/" + tag + ": " + message + "\n");
                if (throwable != null) {
                    writer.write(Log.getStackTraceString(throwable) + "\n");
                }
            } catch (IOException e) {
                Log.w(TAG + "/FusionLogger", "Failed to write log file", e);
                fileLoggingAvailable = false;
            }
        }
    }

    private static File createLogFile() {
        try {
            File dir = new File(Utilities.getExternalFusionCoreDirectory(null), "logs");
            if (!dir.exists() && !dir.mkdirs()) {
                Log.w(TAG + "/FusionLogger", "Could not create log dir: " + dir.getAbsolutePath());
                return null;
            }
            String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File file = new File(dir, "fusion_" + stamp + ".txt");
            try (FileWriter writer = new FileWriter(file, true)) {
                writer.write("=== FusionCore log started " + stamp + " ===\n");
            }
            return file;
        } catch (Exception e) {
            Log.w(TAG + "/FusionLogger", "Could not create log file", e);
            return null;
        }
    }

    private static void pruneOldLogs(File dir) {
        try {
            File[] logs = dir.listFiles((d, name) -> name.startsWith("fusion_") && name.endsWith(".txt"));
            if (logs == null || logs.length <= MAX_KEPT_LOGS) {
                return;
            }
            Arrays.sort(logs, Comparator.comparingLong(File::lastModified));
            for (int i = 0; i < logs.length - MAX_KEPT_LOGS; i++) {
                // Never delete the file we are currently writing to.
                if (!logs[i].equals(currentLogFile) && !logs[i].delete()) {
                    Log.w(TAG + "/FusionLogger", "Could not delete old log: " + logs[i].getName());
                }
            }
        } catch (Exception e) {
            Log.w(TAG + "/FusionLogger", "Could not prune old logs", e);
        }
    }

    /**
     * Returns the newest fusion_*.txt log file, or null if none exists yet.
     */
    public static File getLatestLogFile() {
        try {
            File dir = new File(Utilities.getExternalFusionCoreDirectory(null), "logs");
            File[] logs = dir.listFiles((d, name) -> name.startsWith("fusion_") && name.endsWith(".txt"));
            if (logs == null || logs.length == 0) {
                return null;
            }
            Arrays.sort(logs, Comparator.comparingLong(File::lastModified).reversed());
            return logs[0];
        } catch (Exception e) {
            Log.w(TAG + "/FusionLogger", "Could not find latest log", e);
            return null;
        }
    }
}
