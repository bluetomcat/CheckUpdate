package com.tomcat.checkupdatelibrary.interfaces;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;

/**
 * 创建者：caizongwen
 * 创建时间：2020/11/6
 * 功能描述：
 */
@IntDef({ResultState.RESULT_NO_UPDATE,
        ResultState.RESULT_NEED_UPDATE,
        ResultState.RESULT_SUCCESS,
        ResultState.RESULT_FAIL})
@Retention(RetentionPolicy.RUNTIME)
public @interface ResultType {
}