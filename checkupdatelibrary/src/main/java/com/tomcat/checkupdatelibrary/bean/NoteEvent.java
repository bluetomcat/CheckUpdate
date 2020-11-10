package com.tomcat.checkupdatelibrary.bean;

import com.tomcat.checkupdatelibrary.interfaces.ResultState;
import com.tomcat.checkupdatelibrary.interfaces.ResultType;


/**
 * 创建者：caizongwen
 * 创建时间：2020/11/6
 * 功能描述：
 */
public class NoteEvent {
    private boolean isNeedUp;
    private @ResultType int state;
    private String msg;

    public NoteEvent() {
        this.isNeedUp = false;
        this.state = ResultState.RESULT_NO_UPDATE;
        this.msg = "无更新";
    }

    public NoteEvent(NoteEvent event) {
        this.isNeedUp = event.isNeedUp;
        this.state = event.state;
        this.msg = event.msg;
    }

    public NoteEvent(boolean isNeedUp, int state, String msg) {
        this.isNeedUp = isNeedUp;
        this.state = state;
        this.msg = msg;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public boolean isNeedUp() {
        return isNeedUp;
    }

    public void setNeedUp(boolean needUp) {
        isNeedUp = needUp;
    }

    @Override
    public String toString() {
        return "NoteEvent{" +
                "isNeedUp=" + isNeedUp +
                ", state=" + state +
                ", msg='" + msg + '\'' +
                '}';
    }
}
