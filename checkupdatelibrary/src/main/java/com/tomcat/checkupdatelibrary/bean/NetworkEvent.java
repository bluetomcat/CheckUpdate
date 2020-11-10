package com.tomcat.checkupdatelibrary.bean;

/**
 * 创建者：caizongwen
 * 创建时间：2020/11/9
 * 功能描述：
 */
public class NetworkEvent extends NoteEvent {
    private boolean isAvailable = true;
    private boolean isWifiAvailable = true;
    private boolean isCorrect = true;

    public NetworkEvent() {
        super();
    }

    public NetworkEvent(NoteEvent event) {
        super(event);
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public boolean isWifiAvailable() {
        return isWifiAvailable;
    }

    public void setWifiAvailable(boolean wifiAvailable) {
        isWifiAvailable = wifiAvailable;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public void setCorrect(boolean correct) {
        isCorrect = correct;
    }

    @Override
    public String toString() {
        return "NetworkEvent{" +
                "isAvailable=" + isAvailable +
                ", isWifiAvailable=" + isWifiAvailable +
                ", isCorrect=" + isCorrect +
                ", isNeedUp=" + isNeedUp() +
                ", state=" + getState() +
                ", msg='" + getMsg() + '\'' +
                '}';
    }
}
