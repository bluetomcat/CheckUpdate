package com.tomcat.checkupdate.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Looper;
import android.text.TextUtils;

import com.tomcat.checkupdate.ApkInstallResultFragment;
import com.tomcat.checkupdate.CheckUpdateConfig;
import com.tomcat.checkupdate.interfaces.InstallResultListener;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DefaultObserver;
import okhttp3.HttpUrl;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.Manifest.permission.ACCESS_WIFI_STATE;
import static android.Manifest.permission.INTERNET;
import static android.content.Context.WIFI_SERVICE;

/**
 * 创建者：caizongwen
 * 创建时间：2020/11/8
 * 功能描述：
 */
public class Utils {
    private static final String LINE_SEP = System.getProperty("line.separator");

    public static @NonNull
    <T> T checkNotNull(@Nullable T reference, @NonNull Object errorMessage) {
        if (reference == null) {
            throw new NullPointerException(String.valueOf(errorMessage));
        }
        return reference;
    }

    public static void runOnUiThread(Runnable onComplete) {
        if (onComplete == null) {
            return;
        }
        runOnUiThread(new DefaultObserver<Object>() {
            @Override
            public void onNext(Object o) {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {
                onComplete.run();
            }
        });
    }

    public static void runOnUiThread(Observer<Object> task) {
        if (task == null) {
            return;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.onComplete();
        } else {
            Observable.empty()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(task);
        }
    }

    public static boolean verifyMD5(File packageFile, String refer) {
        try {
            MessageDigest sig = MessageDigest.getInstance("MD5");
            InputStream signedData = new FileInputStream(packageFile);
            byte[] buffer = new byte[4096];//每次检验的文件区大小
            long toRead = packageFile.length();
            long soFar = 0;
            boolean interrupted;
            while (soFar < toRead) {
                interrupted = Thread.interrupted();
                if (interrupted)
                    break;
                int read = signedData.read(buffer);
                soFar += read;
                sig.update(buffer, 0, read);
            }
            byte[] digest = sig.digest();
            String digestStr = byte2HexStr(digest);//将得到的MD5值进行移位转换
            if (digestStr != null
                    && digestStr.toLowerCase().equals(refer.toLowerCase())) {//比较两个文件的MD5值，如果一样则返回true
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public static String byte2HexStr(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (bytes == null || bytes.length <= 0) {
            return null;
        }
        int i = 0;
        while (i < bytes.length) {
            int v;
            String hv;
            v = (bytes[i] >> 4) & 0x0F;
            hv = Integer.toHexString(v);
            stringBuilder.append(hv);

            v = bytes[i] & 0x0F;
            hv = Integer.toHexString(v);
            stringBuilder.append(hv);
            i++;
        }
        return stringBuilder.toString();
    }

    /**
     * 自动安装{注:开始安装前(点击安装页面的安装按钮前)不能释放资源退出程序}
     * 适配android8 检查自动安装权限
     */
    public static boolean installAutoForResult(AppCompatActivity topActivity, File file, InstallResultListener listener) {
        if (topActivity != null) {
            ApkInstallResultFragment value
                    = ApkInstallResultFragment
                    .getLazyApkInstallResultFragment(topActivity.getSupportFragmentManager())
                    .getValue();
            value.installApk(file, checkUpdateAuthorities(), listener);
            return true;
        }
        return false;
    }

    public static String checkUpdateAuthorities() {
        return String.format("%s.checkupdate.provider", CheckUpdateConfig.getInstance().getContext().getPackageName());
    }

    @SuppressWarnings("all")
    public static HttpUrl checkUrl(String url) {
        HttpUrl parseUrl = HttpUrl.parse(url);
        if (null == parseUrl) {
            throw new RuntimeException(url);
        } else {
            return parseUrl;
        }
    }

    @RequiresPermission(ACCESS_NETWORK_STATE)
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null)
            return false;
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI;
    }

    /**
     * Return whether wifi is available.
     * <p>Must hold {@code <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />},
     * {@code <uses-permission android:name="android.permission.INTERNET" />}</p>
     *
     * @return {@code true}: available<br>{@code false}: unavailable
     */
    @RequiresPermission(allOf = {ACCESS_WIFI_STATE, INTERNET})
    @SuppressWarnings("all")
    public static boolean isWifiAvailable(Context context) {
        return getWifiEnabled(context) && isAvailable();
    }

    @RequiresPermission(ACCESS_WIFI_STATE)
    @SuppressWarnings("all")
    public static boolean getWifiEnabled(Context context) {
        @SuppressLint("WifiManagerLeak")
        WifiManager manager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
        if (manager == null)
            return false;
        return manager.isWifiEnabled();
    }

    @RequiresPermission(INTERNET)
    @SuppressWarnings("all")
    public static boolean isAvailable() {
        return isAvailableByDns() || isAvailableByPing(null);
    }

    @RequiresPermission(INTERNET)
    @SuppressWarnings("all")
    public static boolean isAvailableByDns() {
        return isAvailableByDns("");
    }

    /**
     * Return whether network is available using domain.
     * <p>Must hold {@code <uses-permission android:name="android.permission.INTERNET" />}</p>
     *
     * @param domain The name of domain.
     * @return {@code true}: yes<br>{@code false}: no
     */
    @RequiresPermission(INTERNET)
    @SuppressWarnings("all")
    public static boolean isAvailableByDns(final String domain) {
        final String realDomain = TextUtils.isEmpty(domain) ? "www.baidu.com" : domain;
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(realDomain);
            return inetAddress != null;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return false;
    }

    @RequiresPermission(INTERNET)
    public static boolean isAvailableByPing() {
        return isAvailableByPing("");
    }

    /**
     * Return whether network is available using ping.
     * <p>Must hold {@code <uses-permission android:name="android.permission.INTERNET" />}</p>
     *
     * @param ip The ip address.
     * @return {@code true}: yes<br>{@code false}: no
     */
    @RequiresPermission(INTERNET)
    @SuppressWarnings("all")
    public static boolean isAvailableByPing(final String ip) {
        final String realIp = TextUtils.isEmpty(ip) ? "223.5.5.5" : ip;
        CommandResult result = execCmd(String.format("ping -c 1 %s", realIp), false);
        return result.result == 0;
    }

    /**
     * Execute the command.
     *
     * @param command  The command.
     * @param isRooted True to use root, false otherwise.
     * @return the single {@link CommandResult} instance
     */
    @SuppressWarnings("all")
    public static CommandResult execCmd(final String command, final boolean isRooted) {
        return execCmd(new String[]{command}, isRooted, true);
    }

    /**
     * Execute the command.
     *
     * @param commands        The commands.
     * @param isRooted        True to use root, false otherwise.
     * @param isNeedResultMsg True to return the message of result, false otherwise.
     * @return the single {@link CommandResult} instance
     */
    @SuppressWarnings("all")
    public static CommandResult execCmd(final String[] commands,
                                        final boolean isRooted,
                                        final boolean isNeedResultMsg) {
        int result = -1;
        if (commands == null || commands.length == 0) {
            return new CommandResult(result, "", "");
        }
        Process process = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        StringBuilder successMsg = null;
        StringBuilder errorMsg = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec(isRooted ? "su" : "sh");
            os = new DataOutputStream(process.getOutputStream());
            for (String command : commands) {
                if (command == null)
                    continue;
                os.write(command.getBytes());
                os.writeBytes(LINE_SEP);
                os.flush();
            }
            os.writeBytes("exit" + LINE_SEP);
            os.flush();
            result = process.waitFor();
            if (isNeedResultMsg) {
                successMsg = new StringBuilder();
                errorMsg = new StringBuilder();
                successResult = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
                );
                errorResult = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8)
                );
                String line;
                if ((line = successResult.readLine()) != null) {
                    successMsg.append(line);
                    while ((line = successResult.readLine()) != null) {
                        successMsg.append(LINE_SEP).append(line);
                    }
                }
                if ((line = errorResult.readLine()) != null) {
                    errorMsg.append(line);
                    while ((line = errorResult.readLine()) != null) {
                        errorMsg.append(LINE_SEP).append(line);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (successResult != null) {
                    successResult.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (errorResult != null) {
                    errorResult.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (process != null) {
                process.destroy();
            }
        }
        return new CommandResult(
                result,
                successMsg == null ? "" : successMsg.toString(),
                errorMsg == null ? "" : errorMsg.toString()
        );
    }

    public static class CommandResult {
        int result;
        String successMsg;
        String errorMsg;

        CommandResult(final int result, final String successMsg, final String errorMsg) {
            this.result = result;
            this.successMsg = successMsg;
            this.errorMsg = errorMsg;
        }

        @Override
        @SuppressWarnings("all")
        public String toString() {
            return "result: " + result + "\n" +
                    "successMsg: " + successMsg + "\n" +
                    "errorMsg: " + errorMsg;
        }
    }
}
