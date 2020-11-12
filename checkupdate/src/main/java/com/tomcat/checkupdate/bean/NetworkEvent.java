package com.tomcat.checkupdate.bean;

import android.os.Parcel;
import android.os.Parcelable;

import org.jetbrains.annotations.NotNull;

/**
 * 创建者：TomCat0916
 * 创建时间：2020/11/9
 * 功能描述：
 */
public class NetworkEvent extends NoteEvent implements Parcelable {
    private boolean isAvailable = true;
    private boolean isWifiAvailable = true;
    private boolean isCorrect = true;

    public NetworkEvent(Parcel in) {
        super(in);
        this.isAvailable = in.readByte() != 0;
        this.isCorrect = in.readByte() != 0;
        this.isWifiAvailable = in.readByte() != 0;
    }

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

    @NotNull
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeByte((byte) (isAvailable ? 0 : 1));
        dest.writeByte((byte) (isCorrect ? 0 : 1));
        dest.writeByte((byte) (isWifiAvailable ? 0 : 1));
    }
}
