package com.example.stopwatch;

import static com.example.stopwatch.App.CHANNEL_1_ID;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.Notification;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private NotificationManagerCompat managerCompat;
    private EditText mEditTextInput;
    private Button mButtonSet;
    private TextView mTextViewCountDown;
    private Button mButtonStartPause;
    private Button mButtonReset;
    private long mStartTimeMillis;
    private CountDownTimer mCountDownTimer;
    private boolean mTimerRunning;
    private long mTimeLeftInMillis;
    private long endTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextViewCountDown=findViewById(R.id.textViewCountDown);
        mButtonStartPause=findViewById(R.id.button_start_pause);
        mButtonReset=findViewById(R.id.button_reset);
        mEditTextInput=findViewById(R.id.edit_text_input);
        mButtonSet=findViewById(R.id.set_button);
        managerCompat= NotificationManagerCompat.from(this);

        mButtonSet.setOnClickListener(view -> {
            String inputTime=mEditTextInput.getText().toString();
            if(inputTime.length()==0){
                Toast.makeText(this, "Fields can't be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            long millisInput=Long.parseLong(inputTime)*60000;
            if(millisInput==0){
                Toast.makeText(this, "Please enter a positive number", Toast.LENGTH_SHORT).show();
                return;
            }
            setTime(millisInput);
            mEditTextInput.setText("");
        });

        mButtonStartPause.setOnClickListener(view -> {
            if(mTimerRunning){
                pauseTimer();
            }else {
                startTimer();
            }
        });
        mButtonReset.setOnClickListener(view -> {
            resetTimer();
        });
    }

    private void updateCountDownTextView() {
        int hours=(int)(mTimeLeftInMillis/1000)/3600;
        int minutes= (int) (((mTimeLeftInMillis/1000)%3600)/60);
        int seconds= (int) ((mTimeLeftInMillis/1000)%60);
        String leftTimeFormatted;
        if(hours>0){
            leftTimeFormatted=String.format(Locale.getDefault(),"%d:%02d:%02d",hours,minutes,seconds);
        }else {
            leftTimeFormatted=String.format(Locale.getDefault(),"%02d: %02d",minutes,seconds);
        }
        mTextViewCountDown.setText(leftTimeFormatted);

    }
    private void setTime(long milliseconds){
        mStartTimeMillis=milliseconds ;
        resetTimer();
    }

    private void startTimer(){
        endTime=System.currentTimeMillis()+mTimeLeftInMillis;
        mCountDownTimer=new CountDownTimer(mTimeLeftInMillis,1000) {
            @Override
            public void onTick(long  millisUntilFinished) {
                mTimeLeftInMillis= millisUntilFinished;
                updateCountDownTextView();
            }

            @Override
            public void onFinish() {
                mTimerRunning=false;
                updateWatchInterface();
                createNotifications();

            }
        }.start();
        mTimerRunning=true;
        updateWatchInterface();

    }

    private void pauseTimer() {
        mCountDownTimer.cancel();
        mTimerRunning=false;
        updateWatchInterface();

    }
    private void resetTimer(){
        mTimeLeftInMillis=mStartTimeMillis;
        updateCountDownTextView();
        updateWatchInterface();

    }
    private void updateWatchInterface(){
        if(mTimerRunning){
            mEditTextInput.setVisibility(View.INVISIBLE);
            mButtonSet.setVisibility(View.INVISIBLE);
            mButtonStartPause.setText("Pause");
        }else {
            mEditTextInput.setVisibility(View.VISIBLE);
            mButtonSet.setVisibility(View.VISIBLE);
            mButtonStartPause.setText("Start");
            if(mTimeLeftInMillis < 1000){
                mButtonStartPause.setVisibility(View.INVISIBLE);
            }else {
                mButtonStartPause.setVisibility(View.VISIBLE);
            }
            if(mTimeLeftInMillis < mStartTimeMillis){
                mButtonReset.setVisibility(View.VISIBLE);
            }else {
                mButtonReset.setVisibility(View.INVISIBLE);
            }
        }
    }
    private void closeKeyboard(){
        View view=this.getCurrentFocus();
        if(view != null){
            InputMethodManager imm=(InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(),0);
        }
    }
    @Override
    protected void onStop() {
        super.onStop();


        SharedPreferences prefs=getSharedPreferences("prefs",MODE_PRIVATE);
        SharedPreferences.Editor editor=prefs.edit();


        editor.putLong("startTimeMillis",mStartTimeMillis);
        editor.putLong("leftTime",mTimeLeftInMillis);
        editor.putBoolean("timerRunning",mTimerRunning);
        editor.putLong("endTime",endTime);

        editor.apply();

        if (mCountDownTimer != null){
            mCountDownTimer.cancel();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences prefs=getSharedPreferences("prefs",MODE_PRIVATE);

        mStartTimeMillis=prefs.getLong("mStartTimeMillis",600000);
        mTimeLeftInMillis=prefs.getLong("leftTime",mStartTimeMillis);
        mTimerRunning=prefs.getBoolean("timerRunning",false);


        updateCountDownTextView();
        updateWatchInterface();


        if(mTimerRunning){
            endTime=prefs.getLong("endTime",0);
            mTimeLeftInMillis=endTime-System.currentTimeMillis();

            if (mTimeLeftInMillis < 0){
                mTimeLeftInMillis=0;
                mTimerRunning=false;
                updateCountDownTextView();
                updateWatchInterface();
            }else {
                startTimer();
            }
        }
    }
    private void createNotifications(){
        if(!mTimerRunning){

            Uri alarmSound= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_1_ID)
                    .setSmallIcon(R.drawable.ic_one_plus)
                    .setContentTitle("Warning:")
                    .setContentText("Yeh,your countdown is up")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSound(alarmSound)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .build();

            managerCompat.notify(1, notification);
        }
    }
}