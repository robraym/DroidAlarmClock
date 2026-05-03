package ray.droid.com.droidalarmclock;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;

import static android.content.Context.ALARM_SERVICE;

/**
 * Created by Robson on 31/08/2017.
 */

public class Others {

    private static final String NEXT_ALARM_CHANNEL_ID = "next_alarm_channel";
    private static final int NEXT_ALARM_NOTIFICATION_ID = 1002;
    public static final String PREF_NEXT_ALARM_TIME = "NextAlarmTime";

    public static void CancelAlarm(Context context) {
        AlarmManager al = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        Intent mIntent = new Intent(context, AlarmBroadcast.class);

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                0,
                mIntent,
                GetPendingIntentFlags()
        );

        try {
            if (al != null) {
                al.cancel(pi);
            }
            DroidPreferences.SetLong(context, PREF_NEXT_ALARM_TIME, 0);
            CancelNextAlarmNotification(context);
        } catch (Exception ex) {
            Toast.makeText(context, "Não foi possível cancelar o alarme. " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static int GetPendingIntentFlags() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return flags;
    }

    public static Calendar ScheduleAlarm(Context context, ArrayList<Integer> daysOfWeek, int hour, int minute) {
        return ScheduleAlarm(context, daysOfWeek, hour, minute, true);
    }

    public static Calendar ScheduleAlarm(Context context, ArrayList<Integer> daysOfWeek, int hour, int minute, boolean allowImmediateSameMinute) {
        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            throw new IllegalArgumentException("Selecione pelo menos um dia da semana para agendar o alarme.");
        }

        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmBroadcast.class);
        intent.putIntegerArrayListExtra("DaysOfWeek", daysOfWeek);

        PendingIntent alarmIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                GetPendingIntentFlags()
        );

        Calendar calendar = GetNextAlarmTime(daysOfWeek, hour, minute, allowImmediateSameMinute);
        if (alarmMgr != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmMgr.canScheduleExactAlarms()) {
                Intent settingsIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(settingsIntent);
                throw new SecurityException("Permita alarmes e lembretes para o app e tente novamente.");
            }

            PendingIntent showIntent = PendingIntent.getActivity(
                    context,
                    1,
                    new Intent(context, MainActivity.class),
                    GetPendingIntentFlags()
            );
            alarmMgr.setAlarmClock(new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), showIntent), alarmIntent);
            DroidPreferences.SetLong(context, PREF_NEXT_ALARM_TIME, calendar.getTimeInMillis());
            ShowNextAlarmNotification(context, calendar);
        }

        Log.d("DroidAlarmClock", "Próximo alarme em " + DateFormat.format("yyyyMMdd HH:mm", calendar.getTime()).toString());
        return calendar;
    }

    public static String BuildNextAlarmTitle(Calendar alarmTime) {
        return "Próximo alarme: " + DateFormat.format("HH:mm", alarmTime).toString();
    }

    public static String BuildNextAlarmSummary(Calendar alarmTime) {
        long remainingMillis = alarmTime.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
        long totalMinutes = (long) Math.ceil(remainingMillis / 60000.0);

        if (totalMinutes <= 0) {
            totalMinutes = 1;
        }

        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return "Faltam " + FormatRemainingTime(hours, minutes) + " para tocar.";
    }

    private static String FormatRemainingTime(long hours, long minutes) {
        if (hours <= 0) {
            return minutes + " " + (minutes == 1 ? "minuto" : "minutos");
        }

        if (minutes <= 0) {
            return hours + " " + (hours == 1 ? "hora" : "horas");
        }

        return hours + " " + (hours == 1 ? "hora" : "horas")
                + " e " + minutes + " " + (minutes == 1 ? "minuto" : "minutos");
    }

    public static Calendar GetSavedNextAlarm(Context context) {
        long nextAlarmTime = DroidPreferences.GetLong(context, PREF_NEXT_ALARM_TIME, 0);
        if (nextAlarmTime <= Calendar.getInstance().getTimeInMillis()) {
            DroidPreferences.SetLong(context, PREF_NEXT_ALARM_TIME, 0);
            CancelNextAlarmNotification(context);
            return null;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(nextAlarmTime);
        return calendar;
    }

    public static ArrayList<Integer> GetSavedDaysOfWeek(Context context) {
        ArrayList<Integer> days = new ArrayList<>();
        String daysOfWeek = DroidPreferences.GetString(context, "DaysOfWeek");

        for (int day = 1; day <= 7; day++) {
            if (daysOfWeek.contains(String.valueOf(day))) {
                days.add(day);
            }
        }

        return days;
    }

    public static void ShowNextAlarmNotification(Context context, Calendar alarmTime) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        CreateNextAlarmChannel(context);

        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                4,
                mainIntent,
                GetPendingIntentFlags()
        );

        Notification notification = new NotificationCompat.Builder(context, NEXT_ALARM_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(BuildNextAlarmTitle(alarmTime))
                .setContentText(BuildNextAlarmSummary(alarmTime))
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setShowWhen(false)
                .setContentIntent(contentIntent)
                .build();

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NEXT_ALARM_NOTIFICATION_ID, notification);
        }
    }

    public static void CancelNextAlarmNotification(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(NEXT_ALARM_NOTIFICATION_ID);
        }
    }

    private static void CreateNextAlarmChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                NEXT_ALARM_CHANNEL_ID,
                "Próximo alarme",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Mostra o próximo alarme agendado");
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private static Calendar GetNextAlarmTime(ArrayList<Integer> daysOfWeek, int hour, int minute, boolean allowImmediateSameMinute) {
        Calendar now = Calendar.getInstance();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        boolean sameMinute = calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                && calendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                && calendar.get(Calendar.HOUR_OF_DAY) == now.get(Calendar.HOUR_OF_DAY)
                && calendar.get(Calendar.MINUTE) == now.get(Calendar.MINUTE);

        if (allowImmediateSameMinute && sameMinute && (daysOfWeek == null || daysOfWeek.isEmpty() || daysOfWeek.contains(now.get(Calendar.DAY_OF_WEEK)))) {
            calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, 3);
            return calendar;
        }

        if (!calendar.after(now)) {
            calendar.add(Calendar.DATE, 1);
        }

        if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
            while (!daysOfWeek.contains(calendar.get(Calendar.DAY_OF_WEEK))) {
                calendar.add(Calendar.DATE, 1);
            }
        }

        return calendar;
    }
}
