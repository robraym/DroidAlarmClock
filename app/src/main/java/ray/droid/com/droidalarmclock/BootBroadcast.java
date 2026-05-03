package ray.droid.com.droidalarmclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;

public class BootBroadcast extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            long savedNextAlarm = DroidPreferences.GetLong(context, Others.PREF_NEXT_ALARM_TIME, 0);
            ArrayList<Integer> days = Others.GetSavedDaysOfWeek(context);
            if (savedNextAlarm <= 0 || days.isEmpty()) {
                DroidPreferences.SetLong(context, Others.PREF_NEXT_ALARM_TIME, 0);
                Others.CancelNextAlarmNotification(context);
                return;
            }

            int hour = DroidPreferences.GetInteger(context, "timePickerHour");
            int minute = DroidPreferences.GetInteger(context, "timePickerMinute");

            Others.ScheduleAlarm(context, days, hour, minute, false);
            Log.d("DroidAlarmClock", "Alarme restaurado após reinício ou atualização");
        } catch (Exception ex) {
            Log.e("DroidAlarmClock", "Não foi possível restaurar o alarme", ex);
        }
    }
}
