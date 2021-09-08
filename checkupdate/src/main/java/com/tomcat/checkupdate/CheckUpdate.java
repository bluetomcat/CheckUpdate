package com.tomcat.checkupdate;

import android.content.Context;
import android.text.TextUtils;

import com.tomcat.checkupdate.bean.BaseEntity;
import com.tomcat.checkupdate.bean.NetworkEvent;
import com.tomcat.checkupdate.bean.NoteEvent;
import com.tomcat.checkupdate.interfaces.BaseBuilder;
import com.tomcat.checkupdate.interfaces.BaseVersionEntity;
import com.tomcat.checkupdate.interfaces.DownLoadResult;
import com.tomcat.checkupdate.interfaces.InstallResultListener;
import com.tomcat.checkupdate.interfaces.OnShowUiListener;
import com.tomcat.checkupdate.interfaces.ProxyDownLoadCallback;
import com.tomcat.checkupdate.interfaces.ProxyInstallCallback;
import com.tomcat.checkupdate.interfaces.ResultState;
import com.tomcat.checkupdate.utils.AppUtils;
import com.tomcat.checkupdate.utils.FileUtils;
import com.tomcat.checkupdate.utils.LogUtils;
import com.tomcat.checkupdate.utils.Utils;

import java.io.File;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import static android.Manifest.permission.ACCESS_WIFI_STATE;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static java.io.File.separatorChar;

/**
 * 创建者：TomCat0916
 * 创建时间：2020/11/6
 * 功能描述：CheckUpdate 2.0
 * 分离UI和下载逻辑,仅包含检查更新流程/校验逻辑;
 * 示例:
 */
@SuppressWarnings("all")
public class CheckUpdate {

    private BaseVersionEntity data;
    private String url;
    private File apkFile;
    private String cacheDir;
    private String authorities;
    private boolean isForceUpdate;
    private OnShowUiListener<BaseVersionEntity> updateUiListener;
    private OnShowUiListener<BaseVersionEntity> networkUiListener;
    private ProxyDownLoadCallback<BaseVersionEntity> proxyDownLoadCallback;
    private ProxyInstallCallback proxyInstallCallback;
    private WaitOperate<NoteEvent> installOperate;
    private WaitOperate<NoteEvent> updateUiOperate;
    private WaitOperate<NoteEvent> downloadOperate;
    private WaitOperate<NetworkEvent> networkUiOperate;
    private Context context = CheckUpdateConfig.getInstance().getContext();

    private InstallResultListener resultListener = new InstallResultListener() {
        /**
         * 安装结果
         */
        @Override
        public void onResult(boolean isSuccessful) {
            if (installOperate != null) {
                String msg = isSuccessful ? "安装成功" : "安装失败";
                int state = isSuccessful ? ResultState.RESULT_SUCCESS : ResultState.RESULT_FAIL;
                installOperate.goNext(isSuccessful, msg, state);
            }
        }
    };

    private DownLoadResult downLoadResult = new DownLoadResult() {

        @Override
        public void onSuccess(File file) {
            apkFile = file;
            if (downloadOperate != null) {
                downloadOperate.goNext(true, "下载完成");
            }
        }

        @Override
        public void onFailed(Throwable e) {
            if (downloadOperate != null) {
                downloadOperate.goNext(false, e.getMessage());
            }
        }

        @Override
        public void onCanceled() {
            if (downloadOperate != null) {
                downloadOperate.goNext(false, "下载取消");
            }
        }
    };

    public void onDestroy() {
        data = null;
        apkFile = null;
        context = null;
        cacheDir = null;
        destroyOperate(installOperate);
        destroyOperate(updateUiOperate);
        destroyOperate(downloadOperate);
        destroyOperate(networkUiOperate);
        updateUiListener = null;
        networkUiListener = null;
        proxyDownLoadCallback = null;
        downLoadResult = null;
        installOperate = null;
        updateUiOperate = null;
        downloadOperate = null;
        networkUiOperate = null;
        resultListener = null;
        proxyInstallCallback = null;
    }

    CheckUpdate(Builder builder) {
        isForceUpdate = builder.isForceUpdate;
        cacheDir = builder.parentDir == null ? context.getCacheDir().getAbsolutePath() : builder.parentDir.getAbsolutePath();
        if (!TextUtils.isEmpty(builder.apkName)) {
            apkFile = new File(cacheDir, builder.apkName);
        }
        updateUiListener = builder.updateUiListener;
        networkUiListener = builder.networkUiListener;
        proxyDownLoadCallback = builder.proxyDownLoadCallback;
        proxyInstallCallback = builder.proxyInstallCallback;
        authorities = String.format("%s.fileProvider", context.getPackageName());
    }

    private void destroyOperate(WaitOperate operate) {
        if (operate != null) {
            operate.onDestroy();
        }
    }

    @RequiresPermission(allOf = {ACCESS_WIFI_STATE, INTERNET, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE})
    public <E extends BaseEntity> ObservableTransformer<E, NoteEvent> checkUpdate(AppCompatActivity topActicity) {
        return checkUpdate(topActicity, false);
    }

    @RequiresPermission(allOf = {ACCESS_WIFI_STATE, INTERNET, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE})
    public <E extends BaseEntity> ObservableTransformer<E, NoteEvent> checkUpdate(AppCompatActivity topActicity, boolean isIntelligentInstall) {
        return upstream -> upstream.compose(verifyNetVersion())
                .compose(downloadApk())
                .compose(installApk(topActicity, isIntelligentInstall));
    }

    @RequiresPermission(allOf = {READ_EXTERNAL_STORAGE})
    public <E extends BaseEntity> ObservableTransformer<E, NoteEvent> verifyNetVersion() {
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
                                LogUtils.e("showUpdateUi-1", Thread.currentThread().getName(), event);
                                if (event.isNeedUp()) {
                                    if (updateUiOperate == null) {
                                        updateUiOperate = new WaitOperate<>(event);
                                    } else {
                                        updateUiOperate.setLastNote(event);
                                    }
                                    try {
                                        updateUiListener.showUi(updateUiOperate, data);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        updateUiOperate.postError(e);
                                    }
                                    LogUtils.e("showUpdateUi-2", Thread.currentThread().getName(), event);
                                    return updateUiOperate.getNextNoteObservable();
                                }
                                LogUtils.e("showUpdateUi-3", Thread.currentThread().getName(), event);
                                return Observable.just(event);
                            });
        }
        return map;
    }

    /**
     * 权限请求之后调用
     */
    @RequiresPermission(allOf = {READ_EXTERNAL_STORAGE})
    public <E extends BaseEntity> ObservableTransformer<Boolean, NoteEvent> verifyNetVersion(Observable<E> observable) {
        return upstream -> {
            Observable<NoteEvent> map = upstream
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap((Function<Boolean, ObservableSource<NoteEvent>>)
                            aBoolean -> {
                                LogUtils.e("verifyNetVersion-1", Thread.currentThread().getName(), aBoolean);
                                NoteEvent result = new NoteEvent();
                                if (!aBoolean) {
                                    result.setState(ResultState.RESULT_FAIL);
                                    result.setMsg("权限请求失败");
                                }
                                return aBoolean ? observable.map(e -> analyzeNetVersionResult(e, result)) : Observable.just(result);
                            });
            return showUpdateUi(map);
        };
    }

    /**
     * 下载
     */
    @RequiresPermission(allOf = {ACCESS_WIFI_STATE, INTERNET, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE})
    public ObservableTransformer<NoteEvent, NoteEvent> downloadApk() {
        return upstream -> {
            Observable<NetworkEvent> observeOn;
            LogUtils.e("downloadApk-1", Thread.currentThread().getName());
            //网络校验
            return upstream
                    .observeOn(Schedulers.io())
                    .flatMap((Function<NoteEvent, ObservableSource<NetworkEvent>>)
                            noteEvent -> {
                                LogUtils.e("downloadApk-2", Thread.currentThread().getName(), noteEvent);
                                NetworkEvent networkEvent = new NetworkEvent(noteEvent);
                                if (networkEvent.isNeedUp()) {
                                    boolean isCorrect = isCorrect();//验证本地安装文件
                                    networkEvent.setCorrect(isCorrect);
                                    if (!isCorrect) {
                                        boolean isAvailable = Utils.isAvailable();
                                        networkEvent.setAvailable(isAvailable);
                                        if (isAvailable) {
                                            boolean isWifiAvailable = Utils.isWifiConnected(context) && Utils.isWifiAvailable(context);
                                            networkEvent.setWifiAvailable(isWifiAvailable);
                                            networkEvent.setMsg("网络可用,准备下载");
                                            LogUtils.e("downloadApk-3", Thread.currentThread().getName(), networkEvent);
                                            return showNonWifiUi(isAvailable, isWifiAvailable, Observable.just(networkEvent));
                                        } else {
                                            setFail(networkEvent, "网络不可用");
                                        }
                                    } else {
                                        networkEvent.setMsg("本地APk验证成功,准备安装");
                                    }
                                }
                                LogUtils.e("downloadApk-5", Thread.currentThread().getName(), networkEvent);
                                return Observable.just(networkEvent);
                            }).observeOn(AndroidSchedulers.mainThread())
                    .flatMap((Function<NetworkEvent, ObservableSource<NoteEvent>>)
                            event -> {
                                LogUtils.e("downloadApk-6", Thread.currentThread().getName(), event);
                                if (event.isNeedUp() && !event.isCorrect()) {
                                    if (proxyDownLoadCallback != null) {
                                        if (downloadOperate == null) {
                                            downloadOperate = new WaitOperate<>(event);
                                        } else {
                                            downloadOperate.setLastNote(event);
                                        }
                                        try {
                                            proxyDownLoadCallback.onDownload(apkFile, url, downLoadResult, data);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            downloadOperate.postError(e);
                                        }
                                        LogUtils.e("downloadApk-7", Thread.currentThread().getName(), event);
                                        return downloadOperate.getNextNoteObservable();
                                    } else {
                                        setFail(event, "下载代理不能为空");
                                    }
                                }
                                LogUtils.e("downloadApk-8", Thread.currentThread().getName(), event);
                                return Observable.just(event);
                            });
        };
    }

    /**
     * 非WIFI环境提示UI处理
     */
    private Observable<NetworkEvent> showNonWifiUi(boolean isAvailable, boolean isWifiAvailable, Observable<NetworkEvent> just) {
        if (networkUiListener != null && isAvailable && !isWifiAvailable) {
            return just.observeOn(AndroidSchedulers.mainThread())
                    .flatMap((Function<NetworkEvent, ObservableSource<NetworkEvent>>) event -> {
                        LogUtils.e("downloadApk-4", Thread.currentThread().getName(), event);
                        if (networkUiOperate == null) {
                            networkUiOperate = new WaitOperate<>(event);
                        } else {
                            networkUiOperate.setLastNote(event);
                        }
                        try {
                            networkUiListener.showUi(networkUiOperate, data);
                        } catch (Exception e) {
                            e.printStackTrace();
                            networkUiOperate.postError(e);
                        }
                        return networkUiOperate.getNextNoteObservable();
                    });
        } else {
            return just;
        }
    }

    /**
     * 安装
     */
    @RequiresPermission(allOf = {READ_EXTERNAL_STORAGE})
    public ObservableTransformer<NoteEvent, NoteEvent> installApk(AppCompatActivity topActicity, boolean isIntelligentInstall) {
        return upstream -> upstream
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap((Function<NoteEvent, ObservableSource<NoteEvent>>)
                        event -> {
                            LogUtils.e("installApk-1", Thread.currentThread().getName(), event);
                            if (event.isNeedUp()) {
                                if (isCorrect()) {//验证本地安装文件
                                    if (installOperate == null) {
                                        installOperate = new WaitOperate<>(event);
                                    } else {
                                        installOperate.setLastNote(event);
                                    }
                                    try {
                                        if (proxyInstallCallback == null) {
                                            Utils.installAutoForResult(topActicity, apkFile, resultListener);
                                        } else {
                                            proxyInstallCallback.install(apkFile, authorities, isIntelligentInstall, resultListener, data);
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        installOperate.postError(e);
                                    }
                                    LogUtils.e("installApk-2", Thread.currentThread().getName(), event);
                                    return installOperate.getNextNoteObservable();
                                } else {
                                    setFail(event, "安装文件校验失败");
                                }
                            }
                            LogUtils.e("installApk-3", Thread.currentThread().getName(), event);
                            return Observable.just(event);
                        });
    }

    @RequiresPermission(allOf = {READ_EXTERNAL_STORAGE})
    private <E extends BaseEntity<BaseVersionEntity>> NoteEvent analyzeNetVersionResult(E e, NoteEvent result) {
        LogUtils.e("analyzeNetVersionResult-1", Thread.currentThread().getName(), result);
        if (e != null && e.isSuccess()) {
            data = e.getData();
            if (data != null) {
                LogUtils.e(AppUtils.getVersionCode(context),
                        data.valueOfVersionName(data.getVersionName()),
                        data.valueOfVersionName(AppUtils.getVersionName(context)),
                        compareVersionName(data.getVersionName())
                );
            }
            if (data != null
                    && (AppUtils.getVersionCode(context) < data.getVersionCode()
                    || compareVersionName(data.getVersionName()))) {
                try {
                    LogUtils.e(
                            data.getVersionCode(),
                            data.getVersionName(),
                            data.valueOfVersionName(AppUtils.getVersionName(context)),
                            data.valueOfVersionName(data.getVersionName()),
                            AppUtils.getVersionCode(context) < data.getVersionCode(),
                            compareVersionName(data.getVersionName()));
                    url = Utils.checkUrl(data.getAppUrl()).toString();
                    result.setState(ResultState.RESULT_NEED_UPDATE);
                    result.setMsg("准备更新");
                    result.setNeedUp(true);
                    isForceUpdate = isForceUpdate || data.isForceUpdate();
                    result.setForceUpdate(isForceUpdate);
                    String fileName = url.substring(url.lastIndexOf(separatorChar));
                    if (apkFile == null || !fileName.equals(apkFile.getName())) {
                        apkFile = new File(cacheDir, fileName);
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
        LogUtils.e("analyzeNetVersionResult-2", Thread.currentThread().getName(), result);
        return result;
    }

    private boolean compareVersionName(String other) {
        if (data != null) {
            return data.valueOfVersionName(AppUtils.getVersionName(context)) < data.valueOfVersionName(other);
        }
        return false;
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
    @RequiresPermission(allOf = {READ_EXTERNAL_STORAGE})
    private boolean isCorrect() {
        String md5;
        LogUtils.e("isCorrect-1", FileUtils.isFileExists(apkFile), apkFile);
        if (data != null) {
            LogUtils.e(data.md5(), (TextUtils.isEmpty((md5 = data.md5())) || Utils.verifyMD5(apkFile, md5)), Utils.verifyMD5(apkFile, data.md5()));
        }
        if (FileUtils.isFileExists(apkFile) && data != null && (TextUtils.isEmpty((md5 = data.md5())) || Utils.verifyMD5(apkFile, md5))) {
            try {
                AppUtils.AppInfo apkInfo = AppUtils.getApkInfo(context, apkFile);
                long versionCode = AppUtils.getVersionCode(context);
                if (apkInfo != null) {
                    LogUtils.e("isCorrect-2", context.getPackageName().equals(apkInfo.getPackageName()),
                            versionCode < apkInfo.getVersionCode(),
                            compareVersionName(apkInfo.getVersionName()));
                }
                return apkInfo != null
                        && (context.getPackageName().equals(apkInfo.getPackageName()))
                        && (versionCode < apkInfo.getVersionCode() || compareVersionName(apkInfo.getVersionName()));
            } catch (Exception e) {
                e.printStackTrace();
                LogUtils.e(e);
                return false;
            }
        }
        return false;
    }

    public static BaseBuilder builder() {
        return new Builder();
    }

    public interface OperateListener {
        void postNext(boolean isNext, String msg);

        void postNext(boolean isNext);

        void postError(Throwable e);
    }
}
