package com.tomcat.checkupdatelibrary;

import android.app.Application;

import androidx.core.content.FileProvider;

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