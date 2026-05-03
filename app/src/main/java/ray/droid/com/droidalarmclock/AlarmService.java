package ray.droid.com.droidalarmclock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class AlarmService extends Service {

    private static final String CHANNEL_ID = "alarm_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final String ACTION_STOP = "ray.droid.com.droidalarmclock.STOP_ALARM";

    private MediaPlayer player;
    private int previousAlarmVolume = -1;
    private int previousInterruptionFilter = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, buildNotification());
        preparePhoneForAlarm();
        startAlarmSound();
        openAlarmScreen();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopAlarmSound();
        restorePhoneAfterAlarm();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void Stop(Context context) {
        context.stopService(new Intent(context, AlarmService.class));
    }

    private void preparePhoneForAlarm() {
        allowDndInterruption();
        raiseAlarmVolume();
    }

    private void allowDndInterruption() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || !manager.isNotificationPolicyAccessGranted()) {
            return;
        }

        previousInterruptionFilter = manager.getCurrentInterruptionFilter();
        if (previousInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
            try {
                manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
            } catch (SecurityException ex) {
                Log.e("DroidAlarmClock", "Não foi possível alterar o Não perturbe", ex);
            }
        }
    }

    private void raiseAlarmVolume() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) {
            return;
        }

        previousAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        int targetVolume = Math.max(1, maxVolume);
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume, 0);
        } catch (SecurityException ex) {
            Log.e("DroidAlarmClock", "Não foi possível ajustar o volume do alarme", ex);
        }
    }

    private void restorePhoneAfterAlarm() {
        restoreAlarmVolume();
        restoreDndInterruption();
    }

    private void restoreAlarmVolume() {
        if (previousAlarmVolume < 0) {
            return;
        }

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, previousAlarmVolume, 0);
            } catch (SecurityException ex) {
                Log.e("DroidAlarmClock", "Não foi possível restaurar o volume do alarme", ex);
            }
        }
        previousAlarmVolume = -1;
    }

    private void restoreDndInterruption() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || previousInterruptionFilter < 0) {
            return;
        }

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null && manager.isNotificationPolicyAccessGranted()) {
            try {
                manager.setInterruptionFilter(previousInterruptionFilter);
            } catch (SecurityException ex) {
                Log.e("DroidAlarmClock", "Não foi possível restaurar o Não perturbe", ex);
            }
        }
        previousInterruptionFilter = -1;
    }

    private void startAlarmSound() {
        if (player != null && player.isPlaying()) {
            return;
        }

        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        if (alarmUri == null) {
            Log.e("DroidAlarmClock", "Nenhum som de alarme disponível no aparelho");
            return;
        }

        try {
            player = new MediaPlayer();
            player.setDataSource(getApplicationContext(), alarmUri);
            player.setLooping(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                player.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
            } else {
                player.setAudioStreamType(AudioManager.STREAM_ALARM);
            }

            player.prepare();
            player.start();
        } catch (Exception ex) {
            Log.e("DroidAlarmClock", "Não foi possível tocar o alarme", ex);
            stopAlarmSound();
        }
    }

    private void stopAlarmSound() {
        if (player == null) {
            return;
        }

        try {
            if (player.isPlaying()) {
                player.stop();
            }
        } catch (IllegalStateException ignored) {
            // O player pode falhar antes de entrar no estado preparado.
        } finally {
            player.release();
            player = null;
        }
    }

    private void openAlarmScreen() {
        Intent screenIntent = new Intent(this, FullscreenActivity.class);
        screenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(screenIntent);
    }

    private Notification buildNotification() {
        Intent screenIntent = new Intent(this, FullscreenActivity.class);
        screenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent fullScreenIntent = PendingIntent.getActivity(
                this,
                2,
                screenIntent,
                Others.GetPendingIntentFlags()
        );

        Intent stopIntent = new Intent(this, AlarmService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                3,
                stopIntent,
                Others.GetPendingIntentFlags()
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Alarme tocando")
                .setContentText("Toque para abrir o alarme")
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setAutoCancel(false)
                .setFullScreenIntent(fullScreenIntent, true)
                .setContentIntent(fullScreenIntent)
                .addAction(R.mipmap.ic_launcher, "Parar", stopPendingIntent)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Alarme",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Notificação usada quando o alarme está tocando");
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
