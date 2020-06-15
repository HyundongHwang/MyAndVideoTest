package com.hhd.myandvideotest.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class MyUtil {
    private static final String TAG = MyUtil.class.getSimpleName();

    public static ScrollView createScrollViewMpWc(Context context) {
        ScrollView sv = new ScrollView(context);
        ViewGroup.LayoutParams mpwc = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sv.setLayoutParams(mpwc);
        sv.setHorizontalScrollBarEnabled(false);
        sv.setSmoothScrollingEnabled(true);
        sv.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
        return sv;
    }

    public static LinearLayout createLinearLayoutMpWc(Context context) {
        LinearLayout ll = new LinearLayout(context);
        ViewGroup.LayoutParams mpwc = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ll.setLayoutParams(mpwc);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setHorizontalScrollBarEnabled(false);
        return ll;
    }

    public static LinearLayout createActivityGatewayLinearLayout(final Activity activity) {
        LinearLayout ll = MyUtil.createLinearLayoutMpWc(activity);

        try {
            PackageManager pm = activity.getPackageManager();
            String pkgName = activity.getPackageName();
            PackageInfo pi = pm.getPackageInfo(pkgName, PackageManager.GET_ACTIVITIES);

            for (ActivityInfo ai : pi.activities) {
                final Class<?> cls = Class.forName(ai.name);

                if (activity.getClass() == cls)
                    continue;


                Button btn = new Button(activity);
                btn.setAllCaps(false);
                btn.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                btn.setText(cls.getSimpleName());
                ll.addView(btn);

                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.startActivity(new Intent(activity, cls));
                    }
                });
            }
        } catch (Exception ex) {
            Log.e(TAG, ex.toString());
        }

        return ll;
    }

    public static boolean isScreenPortrait(Context context) {
        WindowManager winMgr = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display disp = winMgr.getDefaultDisplay();

        if (disp.getRotation() == Surface.ROTATION_0 ||
                disp.getRotation() == Surface.ROTATION_180) {
            return true;
        }

        return false;
    }
}
