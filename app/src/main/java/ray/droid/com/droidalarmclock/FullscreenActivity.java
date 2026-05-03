package ray.droid.com.droidalarmclock;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;

public class FullscreenActivity extends AppCompatActivity {

    private PowerManager.WakeLock mWakeLock;
    private AnimatorSet pulseAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        findViewById(R.id.btnCancelarAlarme).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopAlarm();
                finish();
            }
        });
        startAlarmPulseAnimation();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "FullscreenActivity");
        mWakeLock.acquire();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

    }

    private void startAlarmPulseAnimation() {
        View outerPulse = findViewById(R.id.alarmPulseOuter);
        View innerPulse = findViewById(R.id.alarmPulseInner);
        View alarmIcon = findViewById(R.id.alarmIcon);

        pulseAnimation = new AnimatorSet();
        pulseAnimation.playTogether(
                createPulseAnimator(outerPulse, 1.0f, 1.24f, 0.36f, 0f, 1400),
                createPulseAnimator(innerPulse, 1.0f, 1.16f, 0.48f, 0.08f, 1100),
                createIconBreathAnimator(alarmIcon)
        );
        pulseAnimation.start();
    }

    private AnimatorSet createPulseAnimator(View view, float startScale, float endScale, float startAlpha, float endAlpha, long duration) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, startScale, endScale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, startScale, endScale);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, startAlpha, endAlpha);

        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        alpha.setRepeatCount(ObjectAnimator.INFINITE);
        scaleX.setRepeatMode(ObjectAnimator.RESTART);
        scaleY.setRepeatMode(ObjectAnimator.RESTART);
        alpha.setRepeatMode(ObjectAnimator.RESTART);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY, alpha);
        animatorSet.setDuration(duration);
        animatorSet.setInterpolator(new LinearInterpolator());
        return animatorSet;
    }

    private AnimatorSet createIconBreathAnimator(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1.0f, 1.05f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1.0f, 1.05f, 1.0f);

        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        scaleX.setRepeatMode(ObjectAnimator.RESTART);
        scaleY.setRepeatMode(ObjectAnimator.RESTART);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(1200);
        animatorSet.setInterpolator(new LinearInterpolator());
        return animatorSet;
    }

    private void stopAlarm() {
        AlarmService.Stop(this);
    }

    @Override
    protected void onDestroy() {
        if (pulseAnimation != null) {
            pulseAnimation.cancel();
        }
        stopAlarm();
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        super.onDestroy();
    }

}
