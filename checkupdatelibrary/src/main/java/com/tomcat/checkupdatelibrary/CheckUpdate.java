package com.tomcat.checkupdatelibrary;

import android.content.Context;
import android.text.TextUtils;

import com.tomcat.checkupdatelibrary.bean.BaseEntity;
import com.tomcat.checkupdatelibrary.bean.NetworkEvent;
import com.tomcat.checkupdatelibrary.bean.NoteEvent;
import com.tomcat.checkupdatelibrary.interfaces.BaseVersionEntity;
import com.tomcat.checkupdatelibrary.interfaces.DownLoadResult;
import com.tomcat.checkupdatelibrary.interfaces.InstallResultListener;
import com.tomcat.checkupdatelibrary.interfaces.OnDownloadListener;
import com.tomcat.checkupdatelibrary.interfaces.OnShowUiListener;
import com.tomcat.checkupdatelibrary.interfaces.ResultState;
import com.tomcat.checkupdatelibrary.utils.AppUtils;
import com.tomcat.checkupdatelibrary.utils.FileUtils;
import com.tomcat.checkupdatelibrary.utils.LogUtils;
import com.tomcat.checkupdatelibrary.utils.Utils;

import java.io.File;

import androidx.appcompat.app.AppCompatActivity;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import static java.io.File.separatorChar;

/**
 * 创建者：TomCat0916
 * 创建时间：2020/11/6
 * 功能描述：CheckUpdate 2.0
 * 分离UI和下载逻辑,仅包含检查更新流程/校验逻辑;
 * 示例:
 * <pre>
 * CheckUpdate<VersionEntity.DataBean> checkUpdate
 *             = new CheckUpdate.Builder<VersionEntity.DataBean>()
 *            .setUpdateUiListener((c, d) -> {
 *                //提示是否更新UI 使用c.postUpdate(isUpdate)通知下一步  可省
 *            })
 *            .setNetworkUiListener((c, d) -> {
 *                //提示是否非WIFI环境下载文件UI 使用c.postDownload(isDownload)通知下一步  可省
 *            })
 *            .setOnDownloadListener((file, url, listener, date) -> {
 *                //下载操作 使用listener.onSuccess()/listener.onFailed(e)/listener.onCanceled()沟通下一步
 *            })
 *            .setCacheDir(getExternalCacheDir())//设置缓存文件夹 可省
 *            .build();
 *
 * BaseApplication.instance.repositoryManager()
 *         .obtainRetrofitService(Api.class)
 *         .getVersionInfo(5)//请求网络版本
 *         .compose(checkUpdate.checkUpdate(this))//检查逻辑(含比较网络版本/下载文件(自定义下载实现)/安装文件) 可拆分并自定义
 *         .subscribe(new SampleObserver<NoteEvent>() {
 *             public void onNext(NoteEvent noteEvent) {
 *                 super.onNext(noteEvent);
 *                 BaseUtils.makeText(noteEvent.getMsg());
 *             }
 *             public void onError(@NotNull Throwable e) {
 *                 super.onError(e);
 *                 BaseUtils.makeText("检查更新失败");
 *             }
 *         });
 * </pre>
 */
public class CheckUpdate<T extends BaseVersionEntity> {

    private T data;
    private String url;
    private File apkFile;
    private File cacheDir;
    private boolean isDebug;
    private boolean isForceUpdate;
    private OnShowUiListener<T> updateUiListener;
    private OnShowUiListener<T> networkUiListener;
    private OnDownloadListener<T> onDownloadListener;
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
        public void onSuccess() {
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

    private CheckUpdate(Builder<T> builder) {
        isForceUpdate = builder.isForceUpdate;
        cacheDir = builder.parentDir;
        if (!FileUtils.isDir(cacheDir)) {
            File externalCacheDir = context.getExternalCacheDir();
            this.cacheDir = externalCacheDir == null ? context.getCacheDir() : externalCacheDir;
        }
        updateUiListener = builder.updateUiListener;
        networkUiListener = builder.networkUiListener;
        onDownloadListener = builder.onDownloadListener;
        isDebug = BuildConfig.DEBUG;
    }

    public void onDestroy() {
        destroyOperate(installOperate);
        destroyOperate(updateUiOperate);
        destroyOperate(downloadOperate);
        destroyOperate(networkUiOperate);
        data = null;
        apkFile = null;
        context = null;
        cacheDir = null;
        downLoadResult = null;
        updateUiListener = null;
        networkUiListener = null;
        onDownloadListener = null;
    }

    private void destroyOperate(WaitOperate operate) {
        if (operate != null) {
            operate.onDestroy();
        }
    }

    public <E extends BaseEntity<T>> ObservableTransformer<E, NoteEvent> checkUpdate(AppCompatActivity activity) {
        return upstream -> upstream.compose(verifyNetVersion())
                .compose(downloadApk())
                .compose(installApk(activity));
    }

    @SuppressWarnings("all")
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
                                debugLog("showUpdateUi-1", Thread.currentThread().getName(), event);
                                if (event.isNeedUp()) {
                                    if (updateUiOperate == null) {
                                        updateUiOperate = new WaitOperate<>(event);
                                    }
                                    try {
                                        updateUiListener.showUi(updateUiOperate, data);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        return Observable.error(e);
                                    }
                                    debugLog("showUpdateUi-2", Thread.currentThread().getName(), event);
                                    return updateUiOperate.getNextNoteObservable();
                                }
                                debugLog("showUpdateUi-3", Thread.currentThread().getName(), event);
                                return Observable.just(event);
                            });
        }
        return map;
    }

    /**
     * 权限请求之后调用
     */
    public <E extends BaseEntity<T>> ObservableTransformer<Boolean, NoteEvent> verifyNetVersion(Observable<E> observable) {
        return upstream -> {
            Observable<NoteEvent> map = upstream
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap((Function<Boolean, ObservableSource<NoteEvent>>)
                            aBoolean -> {
                                debugLog("verifyNetVersion-1", Thread.currentThread().getName(), aBoolean);
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
    @SuppressWarnings("all")
    public ObservableTransformer<NoteEvent, NoteEvent> downloadApk() {
        return upstream -> {
            Observable<NetworkEvent> observeOn;
            debugLog("downloadApk-1", Thread.currentThread().getName());
            //网络校验
            observeOn = upstream
                    .observeOn(Schedulers.io())
                    .flatMap((Function<NoteEvent, ObservableSource<NetworkEvent>>)
                            noteEvent -> {
                                debugLog("downloadApk-2", Thread.currentThread().getName(), noteEvent);
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
                                debugLog("downloadApk-3", Thread.currentThread().getName(), networkEvent);
                                return Observable.just(networkEvent);
                            });
            if (networkUiListener != null) {
                observeOn = observeOn
                        .observeOn(AndroidSchedulers.mainThread())
                        .flatMap((Function<NetworkEvent, ObservableSource<NetworkEvent>>) networkEvent -> {
                            debugLog("downloadApk-4", Thread.currentThread().getName(), networkEvent);
                            if (networkEvent.isNeedUp() && !networkEvent.isCorrect()) {
                                if (networkEvent.isAvailable() && !networkEvent.isWifiAvailable()) {
                                    if (networkUiOperate == null) {
                                        networkUiOperate = new WaitOperate<>(networkEvent);
                                    }
                                    try {
                                        networkUiListener.showUi(networkUiOperate, data);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        return Observable.error(e);
                                    }
                                    debugLog("downloadApk-5", Thread.currentThread().getName(), networkEvent);
                                    return networkUiOperate.getNextNoteObservable();
                                }
                            }
                            debugLog("downloadApk-6", Thread.currentThread().getName(), networkEvent);
                            return Observable.just(networkEvent);
                        });
            }
            return observeOn
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap((Function<NetworkEvent, ObservableSource<NoteEvent>>)
                            event -> {
                                debugLog("downloadApk-7", Thread.currentThread().getName(), event);
                                if (event.isNeedUp() && !event.isCorrect()) {
                                    if (onDownloadListener != null) {
                                        if (downloadOperate == null) {
                                            downloadOperate = new WaitOperate<>(event);
                                        }
                                        try {
                                            onDownloadListener.onDownload(apkFile, url, downLoadResult, data);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            return Observable.error(e);
                                        }
                                        debugLog("downloadApk-8", Thread.currentThread().getName(), event);
                                        return downloadOperate.getNextNoteObservable();
                                    }
                                    setFail(event, "下载监听不能为空");
                                }
                                debugLog("downloadApk-9", Thread.currentThread().getName(), event);
                                return Observable.just(event);
                            });
        };
    }

    /**
     * 安装
     */
    @SuppressWarnings("all")
    public ObservableTransformer<NoteEvent, NoteEvent> installApk(AppCompatActivity topActicity) {
        return upstream -> upstream
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap((Function<NoteEvent, ObservableSource<NoteEvent>>)
                        event -> {
                            debugLog("installApk-1", Thread.currentThread().getName(), event);
                            if (event.isNeedUp()) {
                                if (isCorrect()) {//验证本地安装文件
                                    if (installOperate == null) {
                                        installOperate = new WaitOperate<>(event);
                                    }
                                    try {
                                        Utils.installAutoForResult(topActicity, apkFile, resultListener);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        return Observable.error(e);
                                    }
                                    debugLog("installApk-2", Thread.currentThread().getName(), event);
                                    return installOperate.getNextNoteObservable();
                                } else {
                                    setFail(event, "下载文件校验失败");
                                }
                            }
                            debugLog("installApk-3", Thread.currentThread().getName(), event);
                            return Observable.just(event);
                        });
    }

    private <E extends BaseEntity<T>> NoteEvent analyzeNetVersionResult(E e, NoteEvent result) {
        debugLog("analyzeNetVersionResult-1", Thread.currentThread().getName(), result);
        if (e != null && e.isSuccess()) {
            if ((data = e.getData()) != null
                    && (AppUtils.getVersionCode(context) < data.getVersionCode()
                    || compareVersionName(data.getVersionName()))) {
                try {
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
        debugLog("analyzeNetVersionResult-2", Thread.currentThread().getName(), result);
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
    private boolean isCorrect() {
        String md5;
        debugLog("isCorrect-1", FileUtils.isFileExists(apkFile), data != null ? (TextUtils.isEmpty((md5 = data.md5())) || Utils.verifyMD5(apkFile, md5)) : "null");
        if (FileUtils.isFileExists(apkFile) && data != null && (TextUtils.isEmpty((md5 = data.md5())) || Utils.verifyMD5(apkFile, md5))) {
            try {
                AppUtils.AppInfo apkInfo = AppUtils.getApkInfo(context, apkFile);
                long versionCode = AppUtils.getVersionCode(context);
                if (apkInfo != null) {
                    debugLog("isCorrect-2", context.getPackageName().equals(apkInfo.getPackageName()),
                            versionCode < apkInfo.getVersionCode(),
                            compareVersionName(apkInfo.getVersionName()));
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

    private void debugLog(Object... msg) {
        if (isDebug) {
            LogUtils.e(msg);
        }
    }

    public interface OperateListener {
        void postNext(boolean isNext);

        void postError(Throwable e);
    }

    public static class Builder<T extends BaseVersionEntity> {
        private OnShowUiListener<T> updateUiListener = null;
        private OnShowUiListener<T> networkUiListener = null;
        private OnDownloadListener<T> onDownloadListener = null;
        private boolean isForceUpdate = false;
        private File parentDir = null;

        public Builder<T> setUpdateUiListener(OnShowUiListener<T> updateUiListener) {
            this.updateUiListener = updateUiListener;
            return this;
        }

        public Builder<T> setNetworkUiListener(OnShowUiListener<T> networkUiListener) {
            this.networkUiListener = networkUiListener;
            return this;
        }

        public Builder<T> setOnDownloadListener(OnDownloadListener<T> onDownloadListener) {
            this.onDownloadListener = onDownloadListener;
            return this;
        }

        /**
         * @param isForceUpdate 如果设为true  将禁用更新提示UI用户"否"选项
         */
        public Builder<T> setForceUpdate(boolean isForceUpdate) {
            this.isForceUpdate = isForceUpdate;
            return this;
        }

        public Builder<T> setCacheDir(File parentDir) {
            this.parentDir = parentDir;
            return this;
        }

        public CheckUpdate<T> build() {
            return new CheckUpdate<T>(this);
        }
    }
}

