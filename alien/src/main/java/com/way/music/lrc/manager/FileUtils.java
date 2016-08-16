/**
 * @(#)FileUtils.java    v1.0 2012-12-28
 *
 * Copyright (c) 2012-2012  yunos, Inc. 
 * 2 zijinghua Road, HangZhou, C.N 
 * All rights reserved. 
 */
package com.way.music.lrc.manager;

import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 */
public class FileUtils {
    static final String TAG = "FileUtils";

    public static void writeData(String filePath, byte[] data) {
        if (TextUtils.isEmpty(filePath))
            return;
        OutputStream oS = null;
        try {
            File file = new File(filePath);
            if (!file.exists() && !file.createNewFile())
                return;
            try {
                oS = new FileOutputStream(file);
                oS.write(data);
            } catch (Exception e) {
                Log.e(TAG, "writeData error:" + e.toString());
            } finally {
                try {
                    if (null != oS) oS.close();
                } catch (Exception e1) {
                    Log.e(TAG, "writeData, close output stream error:" + e1.toString());
                }
            }
        } catch (Exception e) {
        }
    }

    public static boolean createDir(String filePath) {
        if (TextUtils.isEmpty(filePath))
            return false;
        File file = new File(filePath);
        String realFilePath = File.separator;
        try {
            if (file.exists())
                return true;

            String[] split = filePath.split(File.separator);
            for (String fileName : split) {
                if (null == fileName)
                    continue;

                realFilePath += fileName + File.separator;
                file = new File(realFilePath);
                if (file.exists())
                    continue;

                file.mkdirs();
                if (VERSION.SDK_INT > VERSION_CODES.FROYO) {
                    file.setReadable(true, false);
                    file.setExecutable(true, false);
                }
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "createDir error:" + e.toString());
        }
        return false;
    }

    public static void deleteFolder(File dir) {
        File filelist[] = dir.listFiles();
        int listlen = filelist.length;
        for (int i = 0; i < listlen; i++) {
            if (filelist[i].isDirectory()) {
                deleteFolder(filelist[i]);
            } else {
                filelist[i].delete();
            }
        }
        dir.delete();
    }

    /**
     * delete files or directories in the specified directory.
     *
     * @param directory the path of directory to be clear.
     * @return
     * @careful
     * @deleteRecursively
     */
    public static boolean ClearDirectory(File directory) {
        boolean result = true;
        if (directory.exists() && directory.isDirectory()) {
            for (File child : directory.listFiles()) {
                result &= delete(child);
            }
            return result;
        } else {
            return false;
        }
    }

    /**
     * must be careful
     *
     * @param path can be a directory or file
     * @return
     * @careful
     * @deleteRecursively
     */
    public static boolean delete(File path) {
        boolean result = true;
        if (path.exists()) {
            if (path.isDirectory()) {
                for (File child : path.listFiles()) {
                    result &= delete(child);
                }
                result &= path.delete(); // Delete empty directory.
            }
            if (path.isFile()) {
                result &= path.delete();
            }
            return result;
        } else {
            return false;
        }
    }

    /**
     * just create a file when it is not exist, nothing will be done if it is exist.<br>
     * The parent folder of the file will be mkdired if it is not exist.
     *
     * @param filePath the absolute path of the file will be created.
     * @return true if create successfully, else false.
     * @helpCreateFolder
     */
    public static boolean newFile(String filePath) {
        if (TextUtils.isEmpty(filePath))
            return false;
        File file = new File(filePath);
        try {
            if (file.exists()) {
                if (file.isDirectory())
                    return false;
                else
                    return true;
            }
            if (!filePath.contains(File.separator) || filePath.endsWith(File.separator))
                return false;
            if (!createDir(filePath.substring(0, filePath.lastIndexOf(File.separator))))
                return false;
            return file.createNewFile();
        } catch (Exception e) {
            Log.e(TAG, "newFile error:" + e.toString());
        }
        return false;
    }

    /**
     * copy file to destination.
     *
     * @param src  the source file
     * @param dest the destination file
     * @return true or false
     */
    public static boolean copyFile(String src, String dest) {
        if (TextUtils.isEmpty(src) || TextUtils.isEmpty(dest))
            return false;
        File srcFile = new File(src);
        if (!srcFile.exists())
            return false;
        InputStream iS = null;
        OutputStream oS = null;
        try {
            iS = new FileInputStream(srcFile);
            if (null != iS) {
                File destFile = new File(dest);
                if (destFile.exists() || destFile.isFile())
                    destFile.deleteOnExit();
                oS = new FileOutputStream(dest);
                //TODO 规范常量
                byte[] buffer = new byte[1024 * 20];
                int size = 0;
                while ((size = iS.read(buffer, 0, buffer.length)) != -1) {
                    oS.write(buffer, 0, size);
                }
                iS.close();
                oS.close();
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "copyFile error:" + e.toString());
        } finally {
            try {
                if (null != iS) iS.close();
                if (null != oS) oS.close();
            } catch (Exception e2) {
                Log.e(TAG, "copyFile, close stream error:" + e2.toString());
            }
        }
        return false;
    }

    /**
     * ensure a file name end with file separator.
     *
     * @param fileName the file name which to be handle.
     * @return the origin value if the fileName is empty string,<br>
     * else ensure that the fileName is end with File.separator
     */
    public static String ensureFileNameEndWithSeparator(String fileName) {
        return (TextUtils.isEmpty(fileName) || fileName.endsWith(File.separator))
                ? fileName : fileName + File.separator;
    }
}
