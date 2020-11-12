package com.tomcat.checkupdate.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;

import java.io.File;

public class AppUtils {


    public static long getVersionCode(Context context) {
        long versionCode = 0L;
        try {
            PackageInfo packageInfo = getPackageInfo(context);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                versionCode = packageInfo.getLongVersionCode();
            } else {
                versionCode = packageInfo.versionCode;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }

    private static PackageInfo getPackageInfo(Context context) throws PackageManager.NameNotFoundException {
        return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
    }

    public static String getVersionName(Context context) {
        String versionName = "";
        try {
            versionName = getPackageInfo(context).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionName;
    }

    public static boolean isSpace(final String s) {
        if (s == null) return true;
        for (int i = 0, len = s.length(); i < len; ++i) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static AppUtils.AppInfo getApkInfo(Context context, final File apkFile) {
        if (apkFile == null || !apkFile.isFile() || !apkFile.exists())
            return null;
        return getApkInfo(context, apkFile.getAbsolutePath());
    }

    @SuppressWarnings("all")
    public static AppUtils.AppInfo getApkInfo(Context context, final String apkFilePath) {
        if (isSpace(apkFilePath))
            return null;
        PackageManager pm = context.getPackageManager();
        if (pm == null)
            return null;
        PackageInfo pi = pm.getPackageArchiveInfo(apkFilePath, 0);
        if (pi == null)
            return null;
        ApplicationInfo appInfo = pi.applicationInfo;
        appInfo.sourceDir = apkFilePath;
        appInfo.publicSourceDir = apkFilePath;
        return getBean(pm, pi);
    }

    private static AppInfo getBean(final PackageManager pm, final PackageInfo pi) {
        if (pi == null) return null;
        ApplicationInfo ai = pi.applicationInfo;
        String packageName = pi.packageName;
        String name = ai.loadLabel(pm).toString();
        Drawable icon = ai.loadIcon(pm);
        String packagePath = ai.sourceDir;
        String versionName = pi.versionName;
        int versionCode = pi.versionCode;
        boolean isSystem = (ApplicationInfo.FLAG_SYSTEM & ai.flags) != 0;
        return new AppInfo(packageName, name, icon, packagePath, versionName, versionCode, isSystem);
    }

    public static class AppInfo {

        private String packageName;
        private String name;
        private Drawable icon;
        private String packagePath;
        private String versionName;
        private int versionCode;
        private boolean isSystem;

        @SuppressWarnings("all")
        public Drawable getIcon() {
            return icon;
        }

        @SuppressWarnings("all")
        public void setIcon(final Drawable icon) {
            this.icon = icon;
        }

        @SuppressWarnings("all")
        public boolean isSystem() {
            return isSystem;
        }

        @SuppressWarnings("all")
        public void setSystem(final boolean isSystem) {
            this.isSystem = isSystem;
        }

        public String getPackageName() {
            return packageName;
        }

        @SuppressWarnings("all")
        public void setPackageName(final String packageName) {
            this.packageName = packageName;
        }

        @SuppressWarnings("all")
        public String getName() {
            return name;
        }

        @SuppressWarnings("all")
        public void setName(final String name) {
            this.name = name;
        }

        @SuppressWarnings("all")
        public String getPackagePath() {
            return packagePath;
        }

        @SuppressWarnings("all")
        public void setPackagePath(final String packagePath) {
            this.packagePath = packagePath;
        }

        public int getVersionCode() {
            return versionCode;
        }

        @SuppressWarnings("all")
        public void setVersionCode(final int versionCode) {
            this.versionCode = versionCode;
        }

        @SuppressWarnings("all")
        public String getVersionName() {
            return versionName;
        }

        @SuppressWarnings("all")
        public void setVersionName(final String versionName) {
            this.versionName = versionName;
        }

        @SuppressWarnings("all")
        public AppInfo(String packageName, String name, Drawable icon, String packagePath,
                       String versionName, int versionCode, boolean isSystem) {
            this.setName(name);
            this.setIcon(icon);
            this.setPackageName(packageName);
            this.setPackagePath(packagePath);
            this.setVersionName(versionName);
            this.setVersionCode(versionCode);
            this.setSystem(isSystem);
        }

        @SuppressWarnings("all")
        @Override
        public String toString() {
            return "{" +
                    "\n  pkg name: " + getPackageName() +
                    "\n  app icon: " + getIcon() +
                    "\n  app name: " + getName() +
                    "\n  app path: " + getPackagePath() +
                    "\n  app v name: " + getVersionName() +
                    "\n  app v code: " + getVersionCode() +
                    "\n  is system: " + isSystem() +
                    "}";
        }
    }
}