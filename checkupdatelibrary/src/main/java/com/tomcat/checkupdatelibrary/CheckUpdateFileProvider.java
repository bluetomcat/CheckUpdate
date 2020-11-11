package com.tomcat.checkupdatelibrary;

import android.app.Application;

import androidx.core.content.FileProvider;

/**
 * 创建者：TomCat0916
 * 创建时间：2020/11/10
 * 功能描述：
 */
public class CheckUpdateFileProvider extends FileProvider {

    @Override
    public boolean onCreate() {
        try {
            CheckUpdateConfig.init((Application) getContext().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}