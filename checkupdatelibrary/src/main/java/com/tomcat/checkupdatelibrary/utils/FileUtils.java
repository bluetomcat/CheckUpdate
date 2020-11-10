package com.tomcat.checkupdatelibrary.utils;

import java.io.File;
import java.io.IOException;

import static com.tomcat.checkupdatelibrary.utils.AppUtils.isSpace;

/**
 * 创建者：caizongwen
 * 创建时间：2020/11/8
 * 功能描述：
 */
public class FileUtils {
    public static File getFileByPath(final String filePath) {
        return isSpace(filePath) ? null : new File(filePath);
    }

    public static boolean isFileExists(final File file) {
        return file != null && file.exists();
    }

    /**
     * Return whether it is a directory.
     *
     * @param file The file.
     * @return {@code true}: yes<br>{@code false}: no
     */
    public static boolean isDir(final File file) {
        return file != null && file.exists() && file.isDirectory();
    }

    /**
     * Delete the file.
     *
     * @param file The file.
     * @return {@code true}: success<br>{@code false}: fail
     */
    public static boolean deleteFile(final File file) {
        return file != null && (!file.exists() || file.isFile() && file.delete());
    }

    public static boolean createOrExistsDir(final File file) {
        return file != null && (file.exists() ? file.isDirectory() : file.mkdirs());
    }

    public static boolean createOrExistsFile(final String filePath) {
        return createOrExistsFile(getFileByPath(filePath));
    }

    /**
     * Create a file if it doesn't exist, otherwise do nothing.
     *
     * @param file The file.
     * @return {@code true}: exists or creates successfully<br>{@code false}: otherwise
     */
    public static boolean createOrExistsFile(final File file) {
        if (file == null) return false;
        if (file.exists()) return file.isFile();
        if (!createOrExistsDir(file.getParentFile())) return false;
        try {
            return file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 提升读写权限
     *
     * @param filePath 文件路径
     */
    public static void setPermission(String filePath) {
        String command = "chmod " + "777" + " " + filePath;
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
