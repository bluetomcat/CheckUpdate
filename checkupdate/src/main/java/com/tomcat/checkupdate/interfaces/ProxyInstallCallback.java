package com.tomcat.checkupdate.interfaces;

import java.io.File;

public interface ProxyInstallCallback<T extends BaseVersionEntity> {

    void install(File apk, String authorities, boolean isIntelligentInstall, InstallResultListener resultListener, T o);
}
