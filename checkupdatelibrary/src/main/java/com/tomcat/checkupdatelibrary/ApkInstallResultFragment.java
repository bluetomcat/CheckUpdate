package com.tomcat.checkupdatelibrary;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tomcat.checkupdatelibrary.interfaces.InstallResultListener;
import com.tomcat.checkupdatelibrary.utils.AppUtils;
import com.tomcat.checkupdatelibrary.utils.FileUtils;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import kotlin.Lazy;
import kotlin.LazyKt;

import static com.tomcat.checkupdatelibrary.utils.FileUtils.setPermission;

/**
 * android8+自动安装适配
 */
public class ApkInstallResultFragment extends Fragment {

    private static final int INSTALL_APK_REQUEST_PERMISSION_CODE = 1010;
    private static final int INSTALL_APK_REQUEST_CODE = 1011;
    private static final String TAG = "ApkInstallResultFragment";
    private File file;
    private String authorities;
    private boolean isSuccessful = false;
    private InstallResultListener listener;
    private static FragmentManager manager = null;

    private ApkInstallResultFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    private static Lazy<ApkInstallResultFragment> fragmentLazy = LazyKt.lazy(() -> {
        if (manager != null) {
            return getApkInstallResultFragment(manager);
        }
        return null;
    });

    @NonNull
    public static Lazy<ApkInstallResultFragment> getLazyApkInstallResultFragment(FragmentManager manager) {
        ApkInstallResultFragment.manager = manager;
        return fragmentLazy;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        intentFilter.addDataScheme("package");
        getContext().registerReceiver(mInstallAppBroadcastReceiver, intentFilter);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private static ApkInstallResultFragment getApkInstallResultFragment(@NonNull final FragmentManager fragmentManager) {
        ApkInstallResultFragment fragment = findApkInstallResultFragment(fragmentManager);
        if (fragment == null) {
            fragment = new ApkInstallResultFragment();
            fragmentManager
                    .beginTransaction()
                    .add(fragment, TAG)
                    .commitNow();
        }
        return fragment;
    }

    private static ApkInstallResultFragment findApkInstallResultFragment(@NonNull final FragmentManager fragmentManager) {
        return (ApkInstallResultFragment) fragmentManager.findFragmentByTag(TAG);
    }

    /**
     * 安装(检查android8+安装授权)
     */
    public void installApk(File file, String authorities, InstallResultListener listener) {
        this.file = file;
        this.authorities = authorities;
        this.listener = listener;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!isHasInstallPermissionWithO()) {
                startInstallPermissionSettingActivity();
                return;
            }
        }
        install();
    }

    /**
     * 执行安装
     */
    private void install() {
        Intent localIntent = new Intent(Intent.ACTION_VIEW);
        localIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        Uri uri;
        setPermission(file.getAbsolutePath());
        if (Build.VERSION.SDK_INT >= 24) {
            uri = FileProvider.getUriForFile(getContext(), authorities, file);
            localIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        } else {
            uri = Uri.fromFile(file);
        }
        localIntent.setDataAndType(uri, "application/vnd.android.package-archive"); //打开apk文件
        startActivityForResult(localIntent, INSTALL_APK_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case INSTALL_APK_REQUEST_PERMISSION_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    install();
                }
                break;
            case INSTALL_APK_REQUEST_CODE:
                if (listener != null) {
                    listener.onResult(isSuccessful);
                }
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean isHasInstallPermissionWithO() {
        return getContext().getPackageManager().canRequestPackageInstalls();
    }

    /**
     * 开启设置安装未知来源应用权限界面
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startInstallPermissionSettingActivity() {
        Uri packageURI = Uri.parse("package:" + getContext().getPackageName());
        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI);
        startActivityForResult(intent, INSTALL_APK_REQUEST_PERMISSION_CODE);
    }

    @Override
    public void onDestroy() {
        if (mInstallAppBroadcastReceiver != null) {
            getContext().unregisterReceiver(mInstallAppBroadcastReceiver);
        }
        if (listener != null) {
            listener.onResult(isSuccessful);
        }
        manager = null;
        super.onDestroy();
    }

    private BroadcastReceiver mInstallAppBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isInstall(intent)) {
                AppUtils.AppInfo apkInfo = AppUtils.getApkInfo(getContext(), file);
                if (intent.getData() != null && apkInfo == null || apkInfo.getPackageName().equals(intent.getData().getSchemeSpecificPart())) {
                    isSuccessful = true;
                    FileUtils.deleteFile(file);
                }
            }
        }
    };

    private boolean isInstall(Intent intent) {
        if (intent != null) {
            return TextUtils.equals(Intent.ACTION_PACKAGE_ADDED, intent.getAction())
                    || TextUtils.equals(Intent.ACTION_PACKAGE_REPLACED, intent.getAction());
        }
        return false;
    }

    @NonNull
    @Override
    public Context getContext() {
        return CheckUpdateConfig.getInstance().getContext();
    }
}