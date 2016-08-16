package com.way.downloadmanager.lib;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

public class FileUtil {

    private static final String TAG = FileUtil.class.getSimpleName();
    private static final String[] FILE_SYSTEM_UNSAFE = {"/", "\\", "..", ":",
            "\"", "?", "*", "<", ">"};
    private static final String[] FILE_SYSTEM_UNSAFE_DIR = {"\\", "..", ":",
            "\"", "?", "*", "<", ">"};

    public static void createDirectoryForParent(File file) {
        File dir = file.getParentFile();
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e(TAG, "Failed to create directory " + dir);
            }
        }
    }

    public static String mendPath(String path) {
        assert path != null : "path!=null";
        String newPath = path;
        if (!newPath.endsWith("/")) {
            newPath += '/';
        }

        return newPath;
    }

    public static boolean ensureDirectoryExistsAndIsReadWritable(File dir) {
        if (dir == null) {
            return false;
        }

        if (dir.exists()) {
            if (!dir.isDirectory()) {
                Log.w(TAG, dir + " exists but is not a directory.");
                return false;
            }
        } else {
            if (dir.mkdirs()) {
                Log.i(TAG, "Created directory " + dir);
            } else {
                Log.w(TAG, "Failed to create directory " + dir);
                return false;
            }
        }

        if (!dir.canRead()) {
            Log.w(TAG, "No read permission for directory " + dir);
            return false;
        }

        if (!dir.canWrite()) {
            Log.w(TAG, "No write permission for directory " + dir);
            return false;
        }
        return true;
    }

    /**
     * Makes a given filename safe by replacing special characters like slashes
     * ("/" and "\") with dashes ("-").
     *
     * @param filename The filename in question.
     * @return The filename with special characters replaced by hyphens.
     */
    private static String fileSystemSafe(String filename) {
        if (filename == null || filename.trim().length() == 0) {
            return "unnamed";
        }

        for (String s : FILE_SYSTEM_UNSAFE) {
            filename = filename.replace(s, "-");
        }
        return filename;
    }

    /**
     * Makes a given filename safe by replacing special characters like colons
     * (":") with dashes ("-").
     *
     * @param path The path of the directory in question.
     * @return The the directory name with special characters replaced by
     * hyphens.
     */
    private static String fileSystemSafeDir(String path) {
        if (path == null || path.trim().length() == 0) {
            return "";
        }

        for (String s : FILE_SYSTEM_UNSAFE_DIR) {
            path = path.replace(s, "-");
        }
        return path;
    }

    /**
     * Similar to {@link java.io.File#listFiles()}, but returns a sorted set. Never
     * returns {@code null}, instead a warning is logged, and an empty set is
     * returned.
     */
    public static SortedSet<File> listFiles(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            Log.w(TAG, "Failed to list children for " + dir.getPath());
            return new TreeSet<File>();
        }

        return new TreeSet<File>(Arrays.asList(files));
    }

    /**
     * Returns the extension (the substring after the last dot) of the given
     * file. The dot is not included in the returned extension.
     *
     * @param name The filename in question.
     * @return The extension, or an empty string if no extension is found.
     */
    public static String getExtension(String name) {
        int index = name.lastIndexOf('.');
        return index == -1 ? "" : name.substring(index + 1).toLowerCase(Locale.getDefault());
    }

    /**
     * Returns the base name (the substring before the last dot) of the given
     * file. The dot is not included in the returned basename.
     *
     * @param name The filename in question.
     * @return The base name, or an empty string if no basename is found.
     */
    public static String getBaseName(String name) {
        int index = name.lastIndexOf('.');
        return index == -1 ? name : name.substring(0, index);
    }

    public static <T extends Serializable> boolean serialize(Context context,
                                                             T obj, String fileName) {
        File file = new File(context.getCacheDir(), fileName);
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream(file));
            out.writeObject(obj);
            Log.i(TAG, "Serialized object to " + file);
            return true;
        } catch (Throwable x) {
            Log.w(LogConst.TAG_RESOURCE, "Caught: " + x, x);
            return false;
        } finally {
            close(out);
        }
    }

    public static <T extends Serializable> T deserialize(Context context,
                                                         String fileName) {
        File file = new File(context.getCacheDir(), fileName);
        if (!file.exists() || !file.isFile()) {
            return null;
        }

        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new FileInputStream(file));
            T result = (T) in.readObject();
            Log.i(TAG, "Deserialized object from " + file);
            return result;
        } catch (Throwable x) {
            Log.w(LogConst.TAG_RESOURCE, "Caught: " + x, x);
            return null;
        } finally {
            close(in);
        }
    }

    public static void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Throwable x) {
            // Ignored
            Log.w(LogConst.TAG_RESOURCE, "Caught: " + x, x);
        }
    }

    public static boolean delete(File file) {
        if (file != null && file.exists()) {
            if (!file.delete()) {
                Log.w(TAG, "Failed to delete file " + file);
                return false;
            }
            Log.i(TAG, "Deleted file " + file);
        }
        return true;
    }

    public static void atomicCopy(File from, File to) throws IOException {
        FileInputStream in = null;
        FileOutputStream out = null;
        File tmp = null;
        try {
            tmp = new File(to.getPath() + ".tmp");
            in = new FileInputStream(from);
            out = new FileOutputStream(tmp);
            in.getChannel().transferTo(0, from.length(), out.getChannel());
            out.close();
            if (!tmp.renameTo(to)) {
                throw new IOException("Failed to rename " + tmp + " to " + to);
            }
            Log.i(TAG, "Copied " + from + " to " + to);
        } catch (IOException x) {
            close(out);
            delete(to);
            throw x;
        } finally {
            close(in);
            close(out);
            delete(tmp);
        }
    }

    public static boolean isUrl(String path) {
        if (null != path) {
            if (path.startsWith("file://")) {
                return false;
            }

            if (path.contains("://")) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasSdcard() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            return true;
        return false;
    }

    public static long getDataIdleSapce() {
        String path = "/data";
        StatFs fileStats = new StatFs(path);
        fileStats.restat(path);
        return fileStats.getAvailableBlocks() * (long) fileStats.getBlockSize();
    }

    public static long getSDCardIdleSpace() {
        if (!hasSdcard())
            return 0;

        String sdcard = getSDCardpath();
        StatFs statFs = new StatFs(sdcard);
        return statFs.getBlockSize() * (long) statFs.getAvailableBlocks();
    }

    public static String getSDCardpath() {
        return Environment.getExternalStorageDirectory().getPath();
    }
}
