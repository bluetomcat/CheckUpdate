package com.tomcat.checkupdate.interfaces;

import com.tomcat.checkupdate.Builder;

public interface BaseBuilder {
    Builder setDownLoadProxy(ProxyDownLoadCallback proxyDownLoadCallback);
}