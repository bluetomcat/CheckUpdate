package com.tomcat.checkupdate.interfaces;

/**
 * 创建者：TomCat0916
 * 创建时间：2020/11/6
 * 功能描述：
 */
public interface ResultState {
    int RESULT_NO_UPDATE = 0;//无更新
    int RESULT_NEED_UPDATE = 1;//准备更新
    int RESULT_SUCCESS = 2;//更新成功
    int RESULT_FAIL = 3;//更新失败
    int RESULT_CANCEL = 4;//取消更新
}
