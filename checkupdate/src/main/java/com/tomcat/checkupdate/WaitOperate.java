package com.tomcat.checkupdate;


import com.tomcat.checkupdate.bean.NetworkEvent;
import com.tomcat.checkupdate.bean.NoteEvent;
import com.tomcat.checkupdate.interfaces.ResultState;
import com.tomcat.checkupdate.utils.LogUtils;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

/**
 * 创建者：TomCat0916
 * 创建时间：2020/11/10
 * 功能描述：用户操作处理代理
 */
public class WaitOperate<N extends NoteEvent> implements CheckUpdate.OperateListener {

    private N lastNote;
    private boolean isDebug;
    private PublishSubject<N> subjects;

    WaitOperate(N note) {
        lastNote = note;
        subjects = PublishSubject.create();
        isDebug = BuildConfig.DEBUG;
    }

    Observable<N> getNextNoteObservable() {
        return Observable.concat(Observable.just(subjects));
    }

    public void onDestroy() {
        if (subjects != null && !subjects.hasComplete()) {
            subjects.onComplete();
        }
        subjects = null;
        lastNote = null;
    }

    @Override
    public void postNext(boolean isNext) {
        next(isNext);
    }

    @Override
    public void postError(Throwable e) {
        checkNotNull();
        subjects.onError(e);
    }

    private void next(boolean isNext) {
        checkNotNull();
        try {
            debugLog(isNext, lastNote);
            if (!isNext) {
                lastNote.setState(ResultState.RESULT_CANCEL);
                lastNote.setMsg("用户取消更新");
            }
            goNext(isNext);
        } catch (Exception e) {
            e.printStackTrace();
            subjects.onError(e);
        }
    }

    private void goNext(boolean isNeedUp) {
        checkNotNull();
        try {
            debugLog(isNeedUp, lastNote);
            goNext(isNeedUp, lastNote.getMsg(), lastNote.getState());
        } catch (Exception e) {
            e.printStackTrace();
            subjects.onError(e);
        }
    }

    void goNext(boolean isNeedUp, String msg) {
        checkNotNull();
        try {
            goNext(isNeedUp, msg, lastNote.getState());
        } catch (Exception e) {
            e.printStackTrace();
            subjects.onError(e);
        }
    }

    void goNext(boolean isNeedUp, String msg, int state) {
        checkNotNull();
        try {
            debugLog(isNeedUp, msg, state, lastNote);
            if (lastNote.isForceUpdate() && !(lastNote instanceof NetworkEvent)) {
                lastNote.setNeedUp(true);
            } else {
                lastNote.setNeedUp(isNeedUp);
            }
            lastNote.setMsg(msg);
            lastNote.setState(state);
            subjects.onNext(lastNote);
            subjects.onComplete();
        } catch (Exception e) {
            e.printStackTrace();
            subjects.onError(e);
        }
    }

    private void checkNotNull() {
        if (subjects == null) {
            throw new NullPointerException("PublishSubject  is Not Null");
        }
    }

    private void debugLog(Object... msg) {
        if (isDebug) {
            LogUtils.e(msg);
        }
    }
}
