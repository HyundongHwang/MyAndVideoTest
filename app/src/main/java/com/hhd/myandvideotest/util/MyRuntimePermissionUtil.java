package com.hhd.myandvideotest.util;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MyRuntimePermissionUtil {
    private static final int _REQ_CODE = 6468;
    private static final String TAG = MyRuntimePermissionUtil.class.getSimpleName();


    public static void check(Activity activity) {
        //http://gun0912.tistory.com/55
        String[] bannedPermArray = MyRuntimePermissionUtil.getBannedPermArray(activity);

        if (bannedPermArray.length == 0)
            return;

        MyRuntimePermissionUtil.request(activity, bannedPermArray);
    }


    //https://developer.android.com/training/permissions/requesting.html?hl=ko
    //https://developer.android.com/guide/topics/security/permissions.html?hl=ko#normal-dangerous
    //http://gun0912.tistory.com/55
    public static String[] getBannedPermArray(Activity activity) {

        try {
            String pkgName = activity.getPackageName();
            PackageInfo myPi = null;
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> riList = activity.getPackageManager().queryIntentActivities(mainIntent, 0);

            for (ResolveInfo ri : riList) {
                try {

                    PackageInfo pi = activity.getPackageManager().getPackageInfo(
                            ri.activityInfo.packageName,
                            PackageManager.GET_PERMISSIONS);

                    if (pi.packageName.contains(pkgName)) {
                        myPi = pi;
                        break;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            ArrayList<String> banedPermList = new ArrayList<>();

            for (String perm : myPi.requestedPermissions) {

                int permCheckResult = ContextCompat.checkSelfPermission(
                        activity,
                        perm);

                if (permCheckResult != PackageManager.PERMISSION_GRANTED) {
                    banedPermList.add(perm);
                }
            }

            return banedPermList.toArray(new String[banedPermList.size()]);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }


    public static void request(Activity activity, String[] banedPermArray) {

        if (banedPermArray.length == 0)
            return;


        ActivityCompat.requestPermissions(
                activity,
                banedPermArray,
                _REQ_CODE);
        // 필요한 권한과 요청 코드를 넣어서 권한허가요청에 대한 결과를 받아야 합니다
    }


    public static void onRequestPermissionsResult(
            Activity activity,
            int requestCode,
            String permissions[],
            int[] grantResults) {

        switch (requestCode) {
            case _REQ_CODE: {

                boolean allPermGranted = false;

                for (int i = 0; i < permissions.length; i++) {
                    String perm = permissions[i];
                    int grantResult = grantResults[i];

                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        // 권한 허가
                        // 해당 권한을 사용해서 작업을 진행할 수 있습니다
                        allPermGranted = true;
                    } else {
                        // 권한 거부
                        // 사용자가 해당권한을 거부했을때 해주어야 할 동작을 수행합니다
                        allPermGranted = false;
                        break;
                    }
                }

                if (!allPermGranted) {
                    activity.finishAffinity();
                }
            }
        }
    }




    public static void req_IGNORE_BATTERY_OPTIMIZATIONS(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return;

        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(Uri.parse("package:" + activity.getPackageName()));
        activity.startActivity(intent);
    }
}
