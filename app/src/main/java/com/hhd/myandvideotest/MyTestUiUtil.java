package com.hhd.myandvideotest;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Method;

public class MyTestUiUtil {
    public interface ILogger {
        void write(String format, Object... args);
        void clear();
    }

    static public LinearLayout createTestLinearLayout(Context context, Activity activity) {
        ViewGroup.LayoutParams lpMpMp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        LinearLayout llRoot = new LinearLayout(context);
        llRoot.setLayoutParams(lpMpMp);
        llRoot.setOrientation(LinearLayout.VERTICAL);

        Class<?> actClass = activity.getClass();
        Method[] methods = actClass.getMethods();

        for (Method m : methods) {
            if (m.getName().startsWith("_test_")) {
                LinearLayout.LayoutParams lpMpWc = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

                Button btn = new Button(context);
                btn.setLayoutParams(lpMpWc);
                btn.setBackgroundColor(0xffcccccc);
                btn.setTextColor(0xff000000);
                btn.setAllCaps(false);
                btn.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                String btnName = m.getName().replace("_test_", "");
                btn.setText(btnName);
                llRoot.addView(btn);

                TextView tv = new TextView(context);
                tv.setTextColor(0xff000000);
                tv.setLayoutParams(lpMpWc);
                llRoot.addView(tv);

                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            m.invoke(activity, new ILogger() {
                                @Override
                                public void write(String format, Object... args) {
                                    String log = String.format(format, args);
                                    String oldLog = tv.getText().toString();

                                    if (MyUtil.isStringNullOrEmpty(oldLog)) {
                                        tv.setText(log);
                                    } else {
                                        tv.setText(String.format("%s\n%s", oldLog, log));
                                    }

                                }

                                @Override
                                public void clear() {
                                    tv.setText("");
                                }
                            });
                        } catch (Exception e) {
                            LogSloth.d(e.toString());
                        }
                    }
                });
            }
        }

        return llRoot;
    }
}
