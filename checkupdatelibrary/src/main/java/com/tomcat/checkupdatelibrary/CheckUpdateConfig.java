package com.tomcat.checkupdatelibrary;

import android.app.Application;

/**
 * 创建者：TomCat0916
 * 创建时间：2020/11/8
 * 功能描述：
 */
public class CheckUpdateConfig {
    private static CheckUpdateConfig instance;
    private static Application application;

    static CheckUpdateConfig init(Application application) {
        CheckUpdateConfig.application = application;
        if (instance == null) {
            synchronized (application.getClass().getName()) {
                if (instance == null) {
                    instance = new CheckUpdateConfig();
                }
            }
        }
        return instance;
    }

    public static CheckUpdateConfig getInstance() {
        if (instance == null) {
            throw new NullPointerException("请先在Application的onCreate初始化");
        }
        return instance;
    }

    public Application getContext() {
        return application;
    }
}
