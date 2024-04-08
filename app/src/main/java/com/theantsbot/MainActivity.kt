package com.theantsbot

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import org.opencv.android.OpenCVLoader


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        OpenCVLoader.initDebug()

        if (!Environment.isExternalStorageManager()) {
            val permissionIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivity(permissionIntent)
        }

        setContentView(R.layout.activity_main)

        val buttonAllowPermission = findViewById(R.id.buttonAllowPermission) as Button;
        buttonAllowPermission.setOnClickListener {
            val dialogIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(dialogIntent)
        }

        val mContext = getApplicationContext();

        val buttonRun = findViewById(R.id.buttonRun) as Button;
        buttonRun.setOnClickListener {
            val packageManager = mContext.getPackageManager()
            val launchIntent = packageManager.getLaunchIntentForPackage("com.star.union.planetant")
            if (launchIntent != null) {
                mContext.startActivity(launchIntent)
            }
            else {
                val launchIntent2 = packageManager.getLaunchIntentForPackage("com.star.union.planetant.flexion")
                if (launchIntent2 != null) {
                    mContext.startActivity(launchIntent2)
                }
                else {
                    Toast.makeText(mContext, "Package not found", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
