package ray.droid.com.droidalarmclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
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
        } catch (Exception ex) {
            Toast.makeText(context, "Nao foi possivel cancelar o alarme. " + ex.getMessage(), Toast.LENGTH_SHORT).show();
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
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmBroadcast.class);
        intent.putIntegerArrayListExtra("DaysOfWeek", daysOfWeek);

        PendingIntent alarmIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                GetPendingIntentFlags()
        );

        Calendar calendar = GetNextAlarmTime(daysOfWeek, hour, minute);
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
        }

        Log.d("DroidAlarmClock", "Proximo alarme em " + DateFormat.format("yyyyMMdd HH:mm", calendar.getTime()).toString());
        return calendar;
    }

    private static Calendar GetNextAlarmTime(ArrayList<Integer> daysOfWeek, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (!calendar.after(Calendar.getInstance())) {
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
