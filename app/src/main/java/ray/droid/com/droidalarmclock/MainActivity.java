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
import android.os.SystemClock;
import android.provider.SyncStateContract;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private Context context;
    private int selectedHour;
    private int selectedMinute;
    private TextView hourPrevious;
    private TextView hourValue;
    private TextView hourNext;
    private TextView minutePrevious;
    private TextView minuteValue;
    private TextView minuteNext;
    private LinearLayout hourColumn;
    private LinearLayout minuteColumn;
    private long segundos = 1000;
    private long minutos = segundos * 5;
    private long horas = minutos * 60;

    private CheckBox chkSeg;
    private CheckBox chkTer;
    private CheckBox chkQua;
    private CheckBox chkQui;
    private CheckBox chkSex;
    private CheckBox chkSab;
    private CheckBox chkDom;
    private Button btnAgendar;
    private int mainScrollDefaultBottomPadding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ConfigureSystemInsets();

        hourPrevious = (TextView) findViewById(R.id.hourPrevious);
        hourValue = (TextView) findViewById(R.id.hourValue);
        hourNext = (TextView) findViewById(R.id.hourNext);
        minutePrevious = (TextView) findViewById(R.id.minutePrevious);
        minuteValue = (TextView) findViewById(R.id.minuteValue);
        minuteNext = (TextView) findViewById(R.id.minuteNext);
        hourColumn = (LinearLayout) findViewById(R.id.hourColumn);
        minuteColumn = (LinearLayout) findViewById(R.id.minuteColumn);
        ConfigureTimeControls();
        Calendar now = Calendar.getInstance();
        selectedHour = now.get(Calendar.HOUR_OF_DAY);
        selectedMinute = now.get(Calendar.MINUTE);
        UpdateTimeControls();
        
        context = getBaseContext();

        chkSeg = (CheckBox) findViewById(R.id.chkSeg);
        chkTer = (CheckBox) findViewById(R.id.chkTer);
        chkQua = (CheckBox) findViewById(R.id.chkQua);
        chkQui = (CheckBox) findViewById(R.id.chkQui);
        chkSex = (CheckBox) findViewById(R.id.chkSex);
        chkSab = (CheckBox) findViewById(R.id.chkSab);
        chkDom = (CheckBox) findViewById(R.id.chkDom);

        btnAgendar = (Button) findViewById(R.id.btnAgendar);

        btnAgendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Others.CancelAlarm(context);
                SetAlarm();
            }
        });

        LoadPreferences();
    }

    private void ConfigureSystemInsets() {
        final ScrollView mainScroll = (ScrollView) findViewById(R.id.mainScroll);
        mainScrollDefaultBottomPadding = mainScroll.getPaddingBottom();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            mainScroll.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                    view.setPadding(
                            view.getPaddingLeft(),
                            view.getPaddingTop(),
                            view.getPaddingRight(),
                            mainScrollDefaultBottomPadding + insets.getSystemWindowInsetBottom()
                    );
                    return insets;
                }
            });
        }
    }

    private void ConfigureTimeControls() {
        hourColumn.setOnTouchListener(CreateTimeColumnTouchListener(true));
        minuteColumn.setOnTouchListener(CreateTimeColumnTouchListener(false));
    }

    private View.OnTouchListener CreateTimeColumnTouchListener(final boolean isHour) {
        return new View.OnTouchListener() {
            private float startY;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startY = event.getY();
                    return true;
                }

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    float deltaY = event.getY() - startY;

                    if (Math.abs(deltaY) > 20) {
                        ChangeTimeWithAnimation(view, isHour, deltaY < 0);
                    } else {
                        ChangeTimeWithAnimation(view, isHour, event.getY() >= view.getHeight() / 2f);
                    }

                    view.performClick();
                    return true;
                }

                return true;
            }
        };
    }

    private void ChangeTimeWithAnimation(final View column, final boolean isHour, final boolean increment) {
        final int direction = increment ? 1 : -1;

        column.animate()
                .translationY(direction * -18)
                .alpha(0.72f)
                .setDuration(80)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (isHour) {
                            selectedHour = WrapValue(selectedHour + direction, 24);
                        } else {
                            selectedMinute = WrapValue(selectedMinute + direction, 60);
                        }

                        UpdateTimeControls();
                        column.setTranslationY(direction * 18);
                        column.animate()
                                .translationY(0)
                                .alpha(1f)
                                .setDuration(90)
                                .start();
                    }
                })
                .start();
    }

    private int WrapValue(int value, int max) {
        if (value < 0) {
            return max - 1;
        }

        if (value >= max) {
            return 0;
        }

        return value;
    }

    private void UpdateTimeControls() {
        hourPrevious.setText(FormatTwoDigits(WrapValue(selectedHour - 1, 24)));
        hourValue.setText(FormatTwoDigits(selectedHour));
        hourNext.setText(FormatTwoDigits(WrapValue(selectedHour + 1, 24)));

        minutePrevious.setText(FormatTwoDigits(WrapValue(selectedMinute - 1, 60)));
        minuteValue.setText(FormatTwoDigits(selectedMinute));
        minuteNext.setText(FormatTwoDigits(WrapValue(selectedMinute + 1, 60)));
    }

    private String FormatTwoDigits(int value) {
        return String.format("%02d", value);
    }

    private void LoadPreferences()
    {

        try {

            String daysOfWeek = DroidPreferences.GetString(context, "DaysOfWeek");

            if (daysOfWeek.contains("1")) {
                chkDom.setChecked(true);
            }
            if (daysOfWeek.contains("2")) {
                chkSeg.setChecked(true);
            }
            if (daysOfWeek.contains("3")) {
                chkTer.setChecked(true);
            }
            if (daysOfWeek.contains("4")) {
                chkQua.setChecked(true);
            }
            if (daysOfWeek.contains("5")) {
                chkQui.setChecked(true);
            }
            if (daysOfWeek.contains("6")) {
                chkSex.setChecked(true);
            }
            if (daysOfWeek.contains("7")) {
                chkSab.setChecked(true);
            }

            int prefHour = DroidPreferences.GetInteger(context, "timePickerHour");
            int prefMinute = DroidPreferences.GetInteger(context, "timePickerMinute");

            selectedHour = prefHour;
            selectedMinute = prefMinute;
            UpdateTimeControls();


        }
        catch (Exception ex)
        {
            Log.d("DroidAlarmClock", "LoadPreferences - Erro: " + ex.getMessage());
        }
    }

    private ArrayList<Integer> SetDaysOfWeek() {
        ArrayList<Integer> days = new ArrayList<>();

        if (chkDom.isChecked()) {
            days.add(1);
        }
        if (chkSeg.isChecked()) {
            days.add(2);
        }
        if (chkTer.isChecked()) {
            days.add(3);
        }
        if (chkQua.isChecked()) {
            days.add(4);
        }
        if (chkQui.isChecked()) {
            days.add(5);
        }
        if (chkSex.isChecked()) {
            days.add(6);
        }
        if (chkSab.isChecked()) {
            days.add(7);
        }
        return days;
    }

    private void SetAlarm() {
        try {
            ArrayList<Integer> daysOfWeek = SetDaysOfWeek();
            DroidPreferences.SetString(context, "DaysOfWeek", daysOfWeek.toString());
            DroidPreferences.SetInteger(context, "timePickerHour", selectedHour);
            DroidPreferences.SetInteger(context, "timePickerMinute", selectedMinute);

            Calendar calendar = Others.ScheduleAlarm(context, daysOfWeek, selectedHour, selectedMinute);

            Toast.makeText(context, "Alarme agendado com sucesso! ", Toast.LENGTH_LONG).show();

            Log.d("DroidAlarmClock", "Alarme agendado em " + DateFormat.format("yyyyMMdd HH:mm", calendar.getTime()).toString());
            finish();
        } catch (Exception ex) {
            Log.e("DroidAlarmClock", "Nao foi possivel agendar o alarme", ex);
            Toast.makeText(context, "Nao foi possivel agendar o alarme. " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


}
