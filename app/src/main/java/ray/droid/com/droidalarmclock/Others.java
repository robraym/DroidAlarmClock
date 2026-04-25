package ray.droid.com.droidalarmclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import static android.content.Context.ALARM_SERVICE;

/**
 * Created by Robson on 31/08/2017.
 */

public class Others {

    public static void CancelAlarm(Context context) {
        AlarmManager al = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        //
        Intent mIntent = new Intent(context, AlarmBroadcast.class);
        PendingIntent pi;

        pi = PendingIntent.getBroadcast(
                context,
                0,
                mIntent,
                GetPendingIntentFlags()
        );

        try {
            al.cancel(pi);
        } catch (Exception ex) {
            Toast.makeText(context, "Não foi possivel cancelar o alarme. " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static int GetPendingIntentFlags() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return flags;
    }




}
