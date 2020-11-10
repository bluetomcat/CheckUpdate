package com.tomcat.checkupdatelibrary;

import android.content.Context;
import android.text.TextUtils;

import com.tomcat.checkupdatelibrary.bean.BaseEntity;
import com.tomcat.checkupdatelibrary.bean.NetworkEvent;
import com.tomcat.checkupdatelibrary.bean.NoteEvent;
import com.tomcat.checkupdatelibrary.interfaces.BaseVersionEntity;
import com.tomcat.checkupdatelibrary.interfaces.DownLoadListener;
import com.tomcat.checkupdatelibrary.interfaces.InstallResultListener;
import com.tomcat.checkupdatelibrary.interfaces.OnDownloadListener;
import com.tomcat.checkupdatelibrary.interfaces.OnShowUiListener;
import com.tomcat.checkupdatelibrary.interfaces.ResultState;
import com.tomcat.checkupdatelibrary.utils.AppUtils;
import com.tomcat.checkupdatelibrary.utils.FileUtils;
import com.tomcat.checkupdatelibrary.utils.LogUtils;
import com.tomcat.checkupdatelibrary.utils.Utils;

import org.jetbrains.annotations.NotNull;

import java.io.File;

import androidx.appcompat.app.AppCompatActivity;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

import static java.io.File.separatorChar;

/**
 * 创建者：caizongwen
 * 创建时间：2020/11/6
 * 功能描述：
 */
public class CheckUpdate<T extends BaseVersionEntity>
        implements InstallResultListener, DownLoadListener {
    private static final String TAG = CheckUpdate.class.getSimpleName();
    private File parentDir;
    private boolean isForceUpdate;
    private OnShowUiListener updateUiListener;
    private OnShowUiListener networkUiListener;
    private OnDownloadListener<T> onDownloadListener;

    private T data;
    private String url;
    private File apkFile;
    private PublishSubject<NoteEvent> installSubjects;
    private PublishSubject<NoteEvent> updateUiSubjects;
    private PublishSubject<NoteEvent> downloadSubjects;
    private PublishSubject<NetworkEvent> NetworkUiSubjects;
    private Context context = CheckUpdateConfig.getInstance().getContext();

    private CheckUpdate(Builder<T> builder) {
        isForceUpdate = builder.isForceUpdate;
        parentDir = builder.parentDir;
        if (!FileUtils.isDir(parentDir)) {
            File externalCacheDir = context.getExternalCacheDir();
            this.parentDir = externalCacheDir == null ? context.getCacheDir() : externalCacheDir;
        }
        updateUiListener = builder.updateUiListener;
        networkUiListener = builder.networkUiListener;
        onDownloadListener = builder.onDownloadListener;
    }


    public void onDestroy() {
        cancelPublishSubject(installSubjects);
        cancelPublishSubject(updateUiSubjects);
        cancelPublishSubject(downloadSubjects);
        cancelPublishSubject(NetworkUiSubjects);
        data = null;
        apkFile = null;
        context = null;
        parentDir = null;
        installSubjects = null;
        updateUiSubjects = null;
        downloadSubjects = null;
        NetworkUiSubjects = null;
        updateUiListener = null;
        networkUiListener = null;
        onDownloadListener = null;
    }


    public <E extends BaseEntity<T>> ObservableTransformer<E, NoteEvent> checkUpdate(AppCompatActivity topActivity) {
        return upstream -> upstream.compose(verifyNetVersion())
                .compose(downloadApk())
                .compose(installApk(topActivity));
    }


    public <E extends BaseEntity<T>> ObservableTransformer<E, NoteEvent> verifyNetVersion() {
        return upstream -> {
            Observable<NoteEvent> map = upstream
                    .map(e -> analyzeNetVersionResult(e, new NoteEvent()))
                    .subscribeOn(Schedulers.io());
            return showUpdateUi(map);
        };
    }

    private Observable<NoteEvent> showUpdateUi(Observable<NoteEvent> map) {
        if (updateUiListener != null) {
            map = map.observeOn(AndroidSchedulers.mainThread())
                    .flatMap((Function<NoteEvent, ObservableSource<NoteEvent>>)
                            event -> {
                                LogUtils.e(TAG, Thread.currentThread().getName(), event);
                                if (event.isNeedUp()) {
                                    if (updateUiSubjects == null) {
                                        updateUiSubjects = PublishSubject.create();
                                    }
                                    try {
                                        updateUiListener.showUi(CheckUpdate.this, data);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        return Observable.error(e);
                                    }
                                    return Observable.concat(Observable.just(updateUiSubjects));
                                }
                                return Observable.just(event);
                            });
        }
        return map;
    }

    @Deprecated
    public <E extends BaseEntity<T>> ObservableTransformer<Boolean, NoteEvent> verifyNetVersion(Observable<E> observable) {
        return upstream -> {
            Observable<NoteEvent> map = upstream
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap((Function<Boolean, ObservableSource<NoteEvent>>)
                            aBoolean -> {
                                LogUtils.e(TAG, Thread.currentThread().getName());
                                NoteEvent result = new NoteEvent();
                                return aBoolean ? Observable.just(result) : observable.map(e -> analyzeNetVersionResult(e, result));
                            });
            return showUpdateUi(map);
        };
    }

    private <E extends BaseEntity<T>> NoteEvent analyzeNetVersionResult(E e, NoteEvent result) {
        LogUtils.e(TAG, Thread.currentThread().getName(), result);
        if (e != null && e.isSuccess()) {
            if ((data = e.getData()) != null
                    && (AppUtils.getVersionCode(context) < data.getVersionCode()
                    || compareVersionName(data.getVersionName()))) {
                try {
                    url = Utils.checkUrl(data.getAppUrl()).toString();
                    result.setState(ResultState.RESULT_NEED_UPDATE);
                    result.setNeedUp(true);
                    isForceUpdate = isForceUpdate || data.isForceUpdate();
                    String fileName = url.substring(url.lastIndexOf(separatorChar));
                    if (apkFile == null || !fileName.equals(apkFile.getName())) {
                        apkFile = new File(parentDir, fileName);
                    }
                } catch (Exception t) {
                    t.printStackTrace();
                    result.setState(ResultState.RESULT_FAIL);
                    result.setMsg("下载地址错误");
                }
            } else {
                result.setState(ResultState.RESULT_NO_UPDATE);
                result.setMsg("无更新");
            }
        } else {
            result.setState(ResultState.RESULT_FAIL);
            result.setMsg("获取网络版本信息失败");
        }
        return result;
    }

    private boolean compareVersionName(String other) {
        if (data != null) {
            return data.valueOfVersionName(AppUtils.getVersionName(context)) < data.valueOfVersionName(other);
        }
        return false;
    }

    /**
     * 下载
     */
    public ObservableTransformer<NoteEvent, NoteEvent> downloadApk() {
        return upstream -> {
            Observable<NetworkEvent> observeOn;
            LogUtils.e(TAG, Thread.currentThread().getName());
            //网络校验
            observeOn = upstream
                    .observeOn(Schedulers.io())
                    .flatMap((Function<NoteEvent, ObservableSource<NetworkEvent>>)
                            noteEvent -> {
                                NetworkEvent networkEvent = new NetworkEvent(noteEvent);
                                if (networkEvent.isNeedUp()) {
                                    boolean isCorrect = isCorrect();//验证本地安装文件
                                    networkEvent.setCorrect(isCorrect);
                                    if (!isCorrect) {
                                        boolean isAvailable = Utils.isAvailable();
                                        networkEvent.setAvailable(isAvailable);
                                        if (isAvailable) {
                                            networkEvent.setWifiAvailable(Utils.isWifiConnected(context) && Utils.isWifiAvailable(context));
                                            return Observable.just(networkEvent);
                                        } else {
                                            setFail(networkEvent, "网络不可用");
                                        }
                                    }
                                }
                                LogUtils.e(TAG, Thread.currentThread().getName(), networkEvent);
                                return Observable.just(networkEvent);
                            });
            if (networkUiListener != null) {
                observeOn = observeOn
                        .observeOn(AndroidSchedulers.mainThread())
                        .flatMap((Function<NetworkEvent, ObservableSource<NetworkEvent>>) networkEvent -> {
                            LogUtils.e(TAG, Thread.currentThread().getName(), networkEvent);
                            if (networkEvent.isNeedUp() && !networkEvent.isCorrect()) {
                                if (networkEvent.isAvailable() && !networkEvent.isWifiAvailable()) {
                                    if (NetworkUiSubjects == null) {
                                        NetworkUiSubjects = PublishSubject.create();
                                    }
                                    try {
                                        networkUiListener.showUi(CheckUpdate.this, data);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        return Observable.error(e);
                                    }
                                    return Observable.concat(Observable.just(NetworkUiSubjects));
                                }
                            }
                            return Observable.just(networkEvent);
                        });
            }
            return observeOn
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap((Function<NetworkEvent, ObservableSource<NoteEvent>>)
                            event -> {
                                LogUtils.e(TAG, Thread.currentThread().getName(), event);
                                if (event.isNeedUp() && !event.isCorrect()) {
                                    //网络下载
                                    if (downloadSubjects == null) {
                                        downloadSubjects = PublishSubject.create();
                                    }
                                    try {
                                        onDownloadListener.onDownload(apkFile, url, this, data);
                                    } catch (Exception e) {
                                        LogUtils.e(e);
                                        e.printStackTrace();
                                        return Observable.error(e);
                                    }
                                    return Observable.concat(Observable.just(downloadSubjects));
                                }
                                return Observable.just(event);
                            });
        };
    }

    /**
     * 安装
     */
    public ObservableTransformer<NoteEvent, NoteEvent> installApk(AppCompatActivity activity) {
        return upstream -> upstream
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap((Function<NoteEvent, ObservableSource<NoteEvent>>)
                        event -> {
                            LogUtils.e(TAG, Thread.currentThread().getName(), event);
                            if (event.isNeedUp()) {
                                if (isCorrect()) {//验证本地安装文件
                                    if (installSubjects == null) {
                                        installSubjects = PublishSubject.create();
                                    }
                                    try {
                                        Utils.installAutoForResult(activity, apkFile, CheckUpdate.this);
                                    } catch (Exception e) {
                                        LogUtils.e(e);
                                        e.printStackTrace();
                                        return Observable.error(e);
                                    }
                                    return Observable.concat(Observable.just(installSubjects));
                                } else {
                                    setFail(event, "下载文件校验失败");
                                }
                            }
                            return Observable.just(event);
                        });
    }

    /**
     * 更新失败
     */
    private void setFail(NoteEvent event, String msg) {
        event.setMsg(msg);
        event.setNeedUp(false);
        event.setState(ResultState.RESULT_FAIL);
    }

    /**
     * 验证本地安装文件是否正确
     */
    private boolean isCorrect() {
        String md5;
        if (FileUtils.isFileExists(apkFile) && data != null && (TextUtils.isEmpty((md5 = data.md5())) || Utils.verifyMD5(apkFile, md5))) {
            try {
                AppUtils.AppInfo apkInfo = AppUtils.getApkInfo(context, apkFile);
                long versionCode = AppUtils.getVersionCode(context);
                if (apkInfo != null) {
                    LogUtils.e((context.getPackageName().equals(apkInfo.getPackageName()))
                            , versionCode < apkInfo.getVersionCode(), compareVersionName(apkInfo.getVersionName()));
                }
                return apkInfo != null
                        && (context.getPackageName().equals(apkInfo.getPackageName()))
                        && (versionCode < apkInfo.getVersionCode() || compareVersionName(apkInfo.getVersionName()));
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    /**
     * 提交更新
     */
    public void postUpdate(boolean isNext) {
        Utils.checkNotNull(updateUiSubjects, getNullMsg("updateUiSubjects"));
        try {
            completePublishSubject(updateUiSubjects, getUserChoose(new NoteEvent(), isNext));
        } catch (Exception e) {
            e.printStackTrace();
            updateUiSubjects.onError(e);
        }
    }

    /**
     * 提交下载Apk
     */
    public void postDownload(boolean isNext) {
        Utils.checkNotNull(NetworkUiSubjects, getNullMsg("NetworkUiSubjects"));
        try {
            completePublishSubject(NetworkUiSubjects, getUserChoose(new NetworkEvent(), isNext));
        } catch (Exception e) {
            e.printStackTrace();
            NetworkUiSubjects.onError(e);
        }
    }

    private <N extends NoteEvent> N getUserChoose(N noteEvent, boolean isNext) {
        noteEvent.setNeedUp(isNext);
        if (!isNext) {
            noteEvent.setState(ResultState.RESULT_CANCEL);
            noteEvent.setMsg("用户取消更新");
        }
        return noteEvent;
    }

    /**
     * 安装结果
     */
    @Override
    public void onResult(boolean isSuccessful) {
        Utils.checkNotNull(installSubjects, getNullMsg("installSubjects"));
        try {
            NoteEvent noteEvent = new NoteEvent();
            noteEvent.setNeedUp(isSuccessful);
            if (isSuccessful) {
                noteEvent.setState(ResultState.RESULT_SUCCESS);
                noteEvent.setMsg("安装成功");
            } else {
                noteEvent.setState(ResultState.RESULT_FAIL);
                noteEvent.setMsg("安装失败");
            }
            completePublishSubject(installSubjects, noteEvent);
        } catch (Exception e) {
            e.printStackTrace();
            installSubjects.onError(e);
        }
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onProgress(int progress) {

    }

    @Override
    public boolean onSuccess() {
        Utils.checkNotNull(downloadSubjects, getNullMsg("downloadSubjects"));
        try {
            NoteEvent noteEvent = new NoteEvent();
            noteEvent.setNeedUp(true);
            noteEvent.setMsg("下载完成");
            completePublishSubject(downloadSubjects, noteEvent);
        } catch (Exception e) {
            e.printStackTrace();
            downloadSubjects.onError(e);
        }
        return false;
    }

    @Override
    public void onFailed(Throwable e) {
        Utils.checkNotNull(downloadSubjects, getNullMsg("downloadSubjects"));
        try {
            NoteEvent noteEvent = new NoteEvent();
            setFail(noteEvent, e.getMessage());
            completePublishSubject(downloadSubjects, noteEvent);
        } catch (Exception ex) {
            ex.printStackTrace();
            downloadSubjects.onError(e);
        }
    }

    @Override
    public void onCanceled() {
        Utils.checkNotNull(downloadSubjects, getNullMsg("downloadSubjects"));
        try {
            NoteEvent noteEvent = new NoteEvent();
            noteEvent.setNeedUp(false);
            noteEvent.setMsg("下载取消");
            noteEvent.setState(ResultState.RESULT_CANCEL);
            completePublishSubject(downloadSubjects, noteEvent);
        } catch (Exception e) {
            e.printStackTrace();
            downloadSubjects.onError(e);
        }
    }

    @NotNull
    private String getNullMsg(String s) {
        return String.format("%s is Null", s);
    }

    private <N extends NoteEvent> void completePublishSubject(PublishSubject<N> subject, N noteEvent) {
        if (subject != null) {
            subject.onNext(noteEvent);
            subject.onComplete();
        }
    }

    private void cancelPublishSubject(PublishSubject subject) {
        if (subject != null) {
            subject.onComplete();
        }
    }

    public static class Builder<T extends BaseVersionEntity> {
        private OnShowUiListener updateUiListener = null;
        private OnShowUiListener networkUiListener = null;
        private OnDownloadListener<T> onDownloadListener = null;
        private boolean isForceUpdate = false;
        private File parentDir = null;

        public Builder<T> setUpdateUiListener(OnShowUiListener updateUiListener) {
            this.updateUiListener = updateUiListener;
            return this;
        }

        public Builder<T> setNetworkUiListener(OnShowUiListener networkUiListener) {
            this.networkUiListener = networkUiListener;
            return this;
        }

        public Builder<T> setOnDownloadListener(OnDownloadListener<T> onDownloadListener) {
            this.onDownloadListener = onDownloadListener;
            return this;
        }

        public Builder<T> setForceUpdate(boolean isForceUpdate) {
            this.isForceUpdate = isForceUpdate;
            return this;
        }

        public Builder<T> setParentDir(File parentDir) {
            this.parentDir = parentDir;
            return this;
        }

        public CheckUpdate<T> build() {
            return new CheckUpdate<T>(this);
        }
    }
}
