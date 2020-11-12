package com.tomcat.checkupdate.bean;

import android.os.Parcel;
import android.os.Parcelable;

import com.tomcat.checkupdate.interfaces.ResultState;
import com.tomcat.checkupdate.interfaces.ResultType;

import org.jetbrains.annotations.NotNull;


/**
 * 创建者：TomCat0916
 * 创建时间：2020/11/6
 * 功能描述：
 */
public class NoteEvent implements Parcelable {
    private boolean isNeedUp;
    private @ResultType
    int state;
    private String msg;
    private boolean isForceUpdate;

    public NoteEvent() {
        this.isNeedUp = false;
        this.state = ResultState.RESULT_NO_UPDATE;
        this.msg = "无更新";
    }

    @SuppressWarnings("all")
    public NoteEvent(NoteEvent event) {
        this.isNeedUp = event.isNeedUp;
        this.state = event.state;
        this.msg = event.msg;
        this.isForceUpdate = event.isForceUpdate;
    }

    public NoteEvent(boolean isNeedUp, int state, String msg) {
        this.isNeedUp = isNeedUp;
        this.state = state;
        this.msg = msg;
    }

    NoteEvent(Parcel in) {
        isNeedUp = in.readByte() != 0;
        state = in.readInt();
        msg = in.readString();
        isForceUpdate = in.readByte() != 0;
    }

    public static final Creator<NoteEvent> CREATOR = new Creator<NoteEvent>() {
        @Override
        public NoteEvent createFromParcel(Parcel in) {
            return new NoteEvent(in);
        }

        @Override
        public NoteEvent[] newArray(int size) {
            return new NoteEvent[size];
        }
    };

    public boolean isForceUpdate() {
        return isForceUpdate;
    }

    public void setForceUpdate(boolean forceUpdate) {
        isForceUpdate = forceUpdate;
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

    @NotNull
    @Override
    public String toString() {
        return "NoteEvent{" +
                "isNeedUp=" + isNeedUp +
                ", isForceUpdate =" + isForceUpdate +
                ", state=" + state +
                ", msg='" + msg + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (isNeedUp ? 0 : 1));
        dest.writeInt(state);
        dest.writeString(msg);
        dest.writeByte((byte) (isForceUpdate ? 0 : 1));
    }
}
