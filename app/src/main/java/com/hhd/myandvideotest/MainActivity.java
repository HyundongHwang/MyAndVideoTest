package com.hhd.myandvideotest;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = this.getBaseContext();
        ViewGroup.LayoutParams lpMpMp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        ScrollView svRoot = new ScrollView(context);
        svRoot.setLayoutParams(lpMpMp);
        this.setContentView(svRoot);
        LinearLayout llRoot = new LinearLayout(context);
        llRoot.setLayoutParams(lpMpMp);
        llRoot.setOrientation(LinearLayout.VERTICAL);
        svRoot.addView(llRoot);

        try {
            PackageManager pm = getPackageManager();
            String packageName = getPackageName();
            PackageInfo pi = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);

            for (ActivityInfo activity : pi.activities) {
                if (!activity.name.contains(packageName))
                    continue;

                Class<?> aClass = Class.forName(activity.name);

                TextView tv = new TextView(context);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.topMargin = (int) (MyUtil.get1DP(context) * 10);
                lp.bottomMargin = (int) (MyUtil.get1DP(context) * 10);
                lp.leftMargin = (int) (MyUtil.get1DP(context) * 10);
                lp.rightMargin = (int) (MyUtil.get1DP(context) * 10);
                tv.setLayoutParams(lp);
                tv.setText(aClass.getSimpleName());
                tv.setTextColor(0xff000000);
                llRoot.addView(tv);
                tv.setTag(aClass);

                tv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Class<?> tvClass = (Class<?>) v.getTag();
                        startActivity(new Intent(context, tvClass));
                    }
                });

                View splitter = new View(context);
                splitter.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (MyUtil.get1DP(context) * 1)));
                splitter.setBackgroundColor(0xff000000);
                llRoot.addView(splitter);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
