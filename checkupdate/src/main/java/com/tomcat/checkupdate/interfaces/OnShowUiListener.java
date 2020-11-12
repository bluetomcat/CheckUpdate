package com.tomcat.checkupdate.interfaces;


import com.tomcat.checkupdate.CheckUpdate;

/**
 * 创建者：TomCat0916
 * 创建时间：2020/11/6
 * 功能描述：
 */
public interface OnShowUiListener<T extends BaseVersionEntity> {
    void showUi(CheckUpdate.OperateListener operate, T o);
}