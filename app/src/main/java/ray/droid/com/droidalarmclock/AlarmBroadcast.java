package ray.droid.com.droidalarmclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by Robson on 28/08/2017.
 */

public class AlarmBroadcast extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ArrayList<Integer> days = intent.getIntegerArrayListExtra("DaysOfWeek");
        if (days == null || days.isEmpty()) {
            DroidPreferences.SetLong(context, Others.PREF_NEXT_ALARM_TIME, 0);
            Others.CancelNextAlarmNotification(context);
            Log.d("DroidAlarmClock", "Alarme ignorado porque nenhum dia da semana foi selecionado");
            return;
        }

        if (checkDayOfWeek(days)) {
            try {
                Log.d("DroidAlarmClock", "AlarmBroadcast - OnReceive");

                Intent serviceIntent = new Intent(context, AlarmService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }

                Log.d("DroidAlarmClock", "startAlarmService");
            } catch (Exception ex) {
                Log.d("DroidAlarmClock", "AlarmBroadcast - OnReceive - Erro: " + ex.getMessage());

            } finally {
                scheduleNextRepeat(context, days);
            }
        }
    }

    private boolean checkDayOfWeek(ArrayList<Integer> days) {
        Calendar calendar = Calendar.getInstance();
        Integer diasemana = calendar.get(Calendar.DAY_OF_WEEK);

        return days.contains(diasemana);
    }

    private void scheduleNextRepeat(Context context, ArrayList<Integer> days) {
        int hour = DroidPreferences.GetInteger(context, "timePickerHour");
        int minute = DroidPreferences.GetInteger(context, "timePickerMinute");
        Others.ScheduleAlarm(context, days, hour, minute, false);
    }
}
