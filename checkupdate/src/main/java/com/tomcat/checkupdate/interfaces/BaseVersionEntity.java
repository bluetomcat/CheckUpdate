package com.tomcat.checkupdate.interfaces;

import android.text.TextUtils;

/**
 * 创建者：TomCat0916
 * 创建时间：2020/11/6
 * 功能描述：
 */
public interface BaseVersionEntity {

    int getVersionCode();

    String getVersionName();

    String getAppUrl();

    String getAppDesc();

    default String md5() {
        return "";
    }

    boolean isForceUpdate();

    default int valueOfVersionName(String versionName) {
        try {
            String trim;
            if (versionName != null && !TextUtils.isEmpty((trim = versionName.trim()))) {
                String replace = trim
                        .replace("v", "")
                        .replace("V", "")
                        .replace(".", "")
                        .replace("_", "");
                return Integer.parseInt(replace);
            }
            return 0;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return 0;
        }
    }
}