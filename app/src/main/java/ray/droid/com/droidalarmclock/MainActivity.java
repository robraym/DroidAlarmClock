package ray.droid.com.droidalarmclock;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
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

import java.util.ArrayList;
import java.util.Calendar;

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
    private LinearLayout nextAlarmCard;
    private TextView nextAlarmTitle;
    private TextView nextAlarmSummary;

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
        RequestNotificationPermission();

        hourPrevious = (TextView) findViewById(R.id.hourPrevious);
        hourValue = (TextView) findViewById(R.id.hourValue);
        hourNext = (TextView) findViewById(R.id.hourNext);
        minutePrevious = (TextView) findViewById(R.id.minutePrevious);
        minuteValue = (TextView) findViewById(R.id.minuteValue);
        minuteNext = (TextView) findViewById(R.id.minuteNext);
        hourColumn = (LinearLayout) findViewById(R.id.hourColumn);
        minuteColumn = (LinearLayout) findViewById(R.id.minuteColumn);
        nextAlarmCard = (LinearLayout) findViewById(R.id.nextAlarmCard);
        nextAlarmTitle = (TextView) findViewById(R.id.nextAlarmTitle);
        nextAlarmSummary = (TextView) findViewById(R.id.nextAlarmSummary);
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
                SetAlarm();
            }
        });

        LoadPreferences();
        UpdateNextAlarmCard();
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

    private void RequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
        }
    }

    private boolean EnsureAlarmCanBypassDoNotDisturb() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || manager.isNotificationPolicyAccessGranted()) {
            return true;
        }

        Toast.makeText(context, "Permita acesso ao Não perturbe para o alarme tocar mesmo no silencioso.", Toast.LENGTH_LONG).show();
        Intent settingsIntent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        startActivity(settingsIntent);
        return false;
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
                    view.getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                    float deltaY = event.getY() - startY;

                    if (Math.abs(deltaY) > 20) {
                        ChangeTimeWithAnimation(view, isHour, deltaY < 0);
                    } else {
                        ChangeTimeWithAnimation(view, isHour, event.getY() >= view.getHeight() / 2f);
                    }

                    view.performClick();
                    return true;
                }

                if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                }

                return true;
            }
        };
    }

    private void ChangeTimeWithAnimation(final View column, final boolean isHour, final boolean increment) {
        final int direction = increment ? 1 : -1;

        if (isHour) {
            selectedHour = WrapValue(selectedHour + direction, 24);
        } else {
            selectedMinute = WrapValue(selectedMinute + direction, 60);
        }
        UpdateTimeControls();

        column.animate()
                .translationY(direction * -18)
                .alpha(0.72f)
                .setDuration(80)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
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

    private String BuildScheduleToast(Calendar alarmTime) {
        return "Alarme agendado para " + DateFormat.format("HH:mm", alarmTime).toString()
                + ". " + Others.BuildNextAlarmSummary(alarmTime);
    }

    private void UpdateNextAlarmCard() {
        Calendar nextAlarm = Others.GetSavedNextAlarm(context);
        if (nextAlarm == null) {
            nextAlarmCard.setVisibility(View.VISIBLE);
            nextAlarmTitle.setText("Nenhum alarme agendado");
            nextAlarmSummary.setText("Toque em Agendar para atualizar");
            return;
        }

        nextAlarmCard.setVisibility(View.VISIBLE);
        nextAlarmTitle.setText(Others.BuildNextAlarmTitle(nextAlarm));
        nextAlarmSummary.setText(Others.BuildNextAlarmSummary(nextAlarm));
        Others.ShowNextAlarmNotification(context, nextAlarm);
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
            if (!EnsureAlarmCanBypassDoNotDisturb()) {
                return;
            }

            ArrayList<Integer> daysOfWeek = SetDaysOfWeek();
            if (daysOfWeek.isEmpty()) {
                ClearAlarmSchedule();
                return;
            }

            DroidPreferences.SetString(context, "DaysOfWeek", daysOfWeek.toString());
            DroidPreferences.SetInteger(context, "timePickerHour", selectedHour);
            DroidPreferences.SetInteger(context, "timePickerMinute", selectedMinute);

            Calendar calendar = Others.ScheduleAlarm(context, daysOfWeek, selectedHour, selectedMinute);
            UpdateNextAlarmCard();

            Toast.makeText(context, BuildScheduleToast(calendar), Toast.LENGTH_LONG).show();

            Log.d("DroidAlarmClock", "Alarme agendado em " + DateFormat.format("yyyyMMdd HH:mm", calendar.getTime()).toString());
            finish();
        } catch (Exception ex) {
            Log.e("DroidAlarmClock", "Não foi possível agendar o alarme", ex);
            Toast.makeText(context, "Não foi possível agendar o alarme. " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }


    }

    private void ClearAlarmSchedule() {
        Others.CancelAlarm(context);
        DroidPreferences.SetString(context, "DaysOfWeek", "");
        DroidPreferences.SetInteger(context, "timePickerHour", selectedHour);
        DroidPreferences.SetInteger(context, "timePickerMinute", selectedMinute);
        DroidPreferences.SetLong(context, Others.PREF_NEXT_ALARM_TIME, 0);
        Others.CancelNextAlarmNotification(context);
        UpdateNextAlarmCard();
        Toast.makeText(context, "Nenhum alarme agendado.", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


}
