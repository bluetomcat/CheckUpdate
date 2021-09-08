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
    private Observable<N> subjects;

    WaitOperate(N note) {
        lastNote = note;
    }

    void setLastNote(N note) {
        onDestroy();
        lastNote = note;
    }

    Observable<N> getNextNoteObservable() {
        if (subjects == null) {
            subjects = PublishSubject.create();
            return Observable.concat(Observable.just(subjects));
        }
        return subjects;
    }

    void onDestroy() {
        onComplete(getPublishSubject());
        subjects = null;
        lastNote = null;
    }

    private void onComplete(PublishSubject<N> publishSubject) {
        if (!(publishSubject == null || publishSubject.hasComplete())) {
            publishSubject.onComplete();
        }
    }

    @Override
    public void postNext(boolean isNext) {
        next(isNext, lastNote == null ? null : lastNote.getMsg());
    }

    @Override
    public void postNext(boolean isNext, String msg) {
        next(isNext, msg);
    }

    @Override
    public void postError(Throwable e) {
        onError(e);
    }

    private void next(boolean isNext, String msg) {
        checkNoteNull();
        try {
            LogUtils.e(isNext, lastNote);
            if (!isNext) {
                lastNote.setState(ResultState.RESULT_CANCEL);
                lastNote.setMsg("用户取消更新");
            }
            goNext(isNext, msg);
        } catch (Exception e) {
            e.printStackTrace();
            onError(e);
        }
    }

    void goNext(boolean isNeedUp, String msg) {
        checkNoteNull();
        try {
            LogUtils.e(isNeedUp, lastNote);
            goNext(isNeedUp, msg, lastNote.getState());
        } catch (Exception e) {
            e.printStackTrace();
            onError(e);
        }
    }

    private void onError(Throwable e) {
        PublishSubject<N> publishSubject = getPublishSubject();
        if (publishSubject != null) {
            publishSubject.onError(e);
            onComplete(publishSubject);
        } else {
            subjects = Observable.error(e);
        }
    }

    void goNext(boolean isNeedUp, String msg, int state) {
        checkNoteNull();
        try {
            LogUtils.e(isNeedUp, msg, state, lastNote);
            if (lastNote.isForceUpdate() && !(lastNote instanceof NetworkEvent)) {
                lastNote.setNeedUp(true);
            } else {
                lastNote.setNeedUp(isNeedUp);
            }
            lastNote.setMsg(msg);
            lastNote.setState(state);
            PublishSubject<N> publishSubject = getPublishSubject();
            if (publishSubject != null) {
                publishSubject.onNext(lastNote);
                onComplete(publishSubject);
            } else {
                subjects = Observable.just(lastNote);
            }
        } catch (Exception e) {
            e.printStackTrace();
            onError(e);
        }
    }

    private PublishSubject<N> getPublishSubject() {
        if (subjects instanceof PublishSubject) {
            return (PublishSubject<N>) this.subjects;
        }
        return null;
    }

    private void checkNoteNull() {
        if (lastNote == null) {
            throw new NullPointerException("lastNote  is Not Null");
        }
    }
}
