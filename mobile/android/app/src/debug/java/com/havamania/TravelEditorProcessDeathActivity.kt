package com.havamania

import android.os.Bundle
import android.os.Process
import android.util.Log

/** Runs the local fixture in its own process and task for adb process-death QA. */
class TravelEditorProcessDeathActivity : TravelEditorTestActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("TripEditorProcessQA", "pid=${Process.myPid()} restored=${savedInstanceState != null}")
    }
}
