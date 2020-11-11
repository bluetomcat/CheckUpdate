package com.tomcat.checkupdatelibrary.interfaces;


import com.tomcat.checkupdatelibrary.CheckUpdate;

/**
 * 创建者：TomCat0916
 * 创建时间：2020/11/6
 * 功能描述：
 */
public interface OnShowUiListener<T extends BaseVersionEntity> {
    void showUi(CheckUpdate.OperateListener operate, T o);
}