package com.hhd.myandvideotest

import android.os.Bundle
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import com.hhd.myandvideotest.util.MyActivityUtil
import com.hhd.myandvideotest.util.MyRuntimePermissionUtil
import com.hhd.myandvideotest.util.MyUtil

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MyRuntimePermissionUtil.check(this)
        MyActivityUtil.setActionBarHide(this)
        MyActivityUtil.setKeepScreenOn(this)

        val sv: ScrollView = MyUtil.createScrollViewMpWc(this)
        this.setContentView(sv)
        val ll_gateway = MyUtil.createActivityGatewayLinearLayout(this)
        sv.addView(ll_gateway)
    }
}

