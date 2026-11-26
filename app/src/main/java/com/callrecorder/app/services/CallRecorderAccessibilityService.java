package com.callrecorder.app.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.util.Log;

import com.callrecorder.app.utils.DiagLog;
import android.view.accessibility.AccessibilityEvent;

/**
 * Accessibility service used as a fallback mechanism to detect call state changes
 * on devices where PHONE_STATE broadcast is restricted.
 */
public class CallRecorderAccessibilityService extends AccessibilityService {
    private static final String TAG = "A11yCallRecorder";

    @Override
    public void onServiceConnected() {
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        setServiceInfo(info);
        DiagLog.log(this, TAG, "Accessibility service connected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Reserved for future call detection enhancements on restricted devices
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted");
    }
}
