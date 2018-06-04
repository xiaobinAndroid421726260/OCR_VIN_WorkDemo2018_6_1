package com.example.ocr.ocr.ui.util;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by Administrator on 2017/6/1 0001.
 */

public class SystemUtils {

    //因为是工具类，所以需要设置成静态的
    public static String getMobileName() {
        //build是一个类，dalvik系统模式，此类用于获取系统的一些属性,
        //其中brand获取到的是一个String类型的手机名称，所以需要返回值
        return Build.BRAND;
    }

    //获取手机型号
    public static String getMobileModle() {
        return Build.MODEL;
    }

    //获取系统版本号
    public static String getSystemVersion() {
        return Build.VERSION.RELEASE;
    }

    //获取手机型号和系统版本
    public static String getModelAndVersion() {
        return getMobileModle() + "Android" + getSystemVersion();
    }

    //获取手机的基带版本
    public static String getBaseband() {
        return Build.getRadioVersion();
    }

    //获取CPU名字
    public static String getCPUName() {
        FileReader reader = null;
        BufferedReader br = null;
        try {
            //读取"/proc/cpuinfo"文件夹下面的文件
            reader = new FileReader("/proc/cpuinfo");
            //因为要读取一行，所以需要包装
            br = new BufferedReader(reader);
            String str = br.readLine();
            //拆分字符串,使用冒号空格来拆分
            String[] str1 = str.split(":\\s+");
            return str1[1];//需要下标为1的位置上的内容
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                //关闭流释放资源
                if (br != null) {
                    br.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "unknown";
    }

    //获取cpu的核心数
    public static int getCPUNum() {
        //方法内部类
        class MyFilter implements FileFilter {
            @Override//指定过滤格式，也就是过滤规则，参数是文件名字
            public boolean accept(File pathname) {
                if (Pattern.matches("cpu[0-9]", pathname.getName())) {
                    return true;
                }
                return false;
            }
        }
        //是一个相对路径，所以前面的/不能少
        File dir = new File("/sys/devices/system/cpu/");
        //列出指定目录下所有文件及文件夹
        File[] files = dir.listFiles(new MyFilter());
        return files.length;
    }

    //获取手机屏幕的分辨率
    public static String getScreenPx(Activity activity) {
        //DisplayMetrics:显示矩阵，屏幕显示区域
        //Display可能是屏幕显示区域，也可能代指应用程序的显示区域
        DisplayMetrics outMetrics = new DisplayMetrics();
        //获取手机的矩阵
        activity.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.widthPixels + "*" + outMetrics.heightPixels;
    }


    /**
     * 判断应用是否已经启动
     *
     * @param context     一个context
     * @param packageName 要判断应用的包名
     * @return boolean
     */
    public static boolean isAppAlive(Context context, String packageName) {
        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processInfos
                = activityManager.getRunningAppProcesses();
        for (int i = 0; i < processInfos.size(); i++) {
            if (processInfos.get(i).processName.equals(packageName)) {
                Log.i("NotificationLaunch",
                        String.format("the %s is running, isAppAlive return true", packageName));
                return true;
            }
        }
        Log.i("NotificationLaunch",
                String.format("the %s is not running, isAppAlive return false", packageName));
        return false;
    }


    /**
     * 判断某个界面是否在前台
     *
     * @param activity 要判断的Activity
     * @return 是否在前台显示
     */
    public static boolean isForeground(Activity activity) {
        return isForeground(activity, activity.getClass().getName());
    }

    /**
     * 判断某个界面是否在前台
     *
     * @param context   Context
     * @param className 界面的类名
     * @return 是否在前台显示
     */
    public static boolean isForeground(Context context, String className) {
        if (context == null || TextUtils.isEmpty(className))
            return false;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(1);
        if (list != null && list.size() > 0) {
            ComponentName cpn = list.get(0).topActivity;
            if (className.equals(cpn.getClassName()))
                return true;
        }
        return false;
    }

    // 设置状态栏透明度颜色
    public static void changeWindow(Context context, int color) {

        Window window = ((Activity)context).getWindow();
        //4.4版本及以上
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.setFlags(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        //5.0版本及以上
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //取消设置透明状态栏,使 ContentView 内容不再覆盖状态栏
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION (在华为手机上底部视图会被导航栏遮挡)
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            //需要设置这个 flag 才能调用 setStatusBarColor 来设置状态栏颜色
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            //设置状态栏颜色
            window.setStatusBarColor(color);
        }

        SystemBarTintManager tintManager = new SystemBarTintManager((Activity)context);
        //开启SystemBarTint
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setStatusBarTintColor(color); //通知栏所需颜色
//        tintManager.setStatusBarTintDrawable(); //通知栏所需颜色
//        tintManager.setStatusBarTintResource(); //通知栏所需颜色
    }


    /**
     * 获取手机IMEI号
     */
    public static String getIMEI(Context context) {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(context.TELEPHONY_SERVICE);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                // 6.0权限问题的处理
                PermissionManager.getInstance(context)
                        .execute((Activity) context, Manifest.permission.READ_PHONE_STATE);
            }
            return telephonyManager.getDeviceId();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 获取手机IMSI号
     */
    public static String getIMSI(Context context) {
        try {
            TelephonyManager mTelephonyMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                // 6.0权限问题的处理
                PermissionManager.getInstance(context)
                        .execute((Activity) context, Manifest.permission.READ_PHONE_STATE);
            }
            return mTelephonyMgr.getSubscriberId();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 通过网络接口取
     *
     * @return MAC
     */
    public static String getNewMac() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return null;
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:", b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }


    /**
     * mac去掉分隔符(:)
     */
    public static String getMac(String mac) {
        String result = "";
        try {
            if (mac != null && mac.length() > 1) {
                mac = mac.replaceAll(" ", "");
                result = "";
                String[] tmp = mac.split(":");
                for (int i = 0; i < tmp.length; ++i) {
                    result += tmp[i];
                }
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
