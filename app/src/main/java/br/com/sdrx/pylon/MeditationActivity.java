package br.com.sdrx.pylon;

import androidx.appcompat.app.AppCompatActivity;
        import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.Notification;
import android.app.NotificationChannel;
        import android.app.NotificationManager;
        import android.app.PendingIntent;
        import android.content.Context;
        import android.content.Intent;
        import android.media.Ringtone;
        import android.media.RingtoneManager;
        import android.net.Uri;
        import android.os.Build;
        import android.os.Bundle;
        import android.os.CountDownTimer;
        import android.util.Log;
        import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;
        import android.widget.Toast;
        import android.widget.Button;

        import java.io.BufferedReader;
        import java.io.IOException;
        import java.io.InputStreamReader;
        import java.io.OutputStream;
        import java.net.HttpURLConnection;
        import java.net.URL;
        import java.text.SimpleDateFormat;
        import java.util.Date;
        import java.util.Objects;

public class MeditationActivity extends AppCompatActivity {
    private static final String CHANNEL_ID = "pylon_notifications";

    private CountDownTimer countDownTimer;
    private long timeLeft = 1 * 10 * 1000; // 1 minute
    private boolean isTimerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meditation);
        createNotificationChannel();
        setNumberPickerValues();
    }

    private void setNumberPickerValues() {
        NumberPicker minsNumberPicker = (NumberPicker) findViewById(R.id.meditation_numberpicker_minutes);
        NumberPicker secsNumberPicker = (NumberPicker) findViewById(R.id.meditation_numberpicker_seconds);

        minsNumberPicker.setMinValue(0);
        minsNumberPicker.setMaxValue(100);
        secsNumberPicker.setMinValue(0);
        secsNumberPicker.setMaxValue(59);
    }

    private Long getMillisecondsFromNumberPickers() {
        NumberPicker minsNumberPicker = (NumberPicker) findViewById(R.id.meditation_numberpicker_minutes);
        NumberPicker secsNumberPicker = (NumberPicker) findViewById(R.id.meditation_numberpicker_seconds);

        int mins = minsNumberPicker.getValue();
        int secs = secsNumberPicker.getValue();

        return (long) (mins * 60 * 1000) + (secs * 1000);
    }

    public void startMeditate(View view) {
        TextView timerTextView = (TextView) findViewById(R.id.timerTextView);
        Button button = (Button) view;
        if (!isTimerRunning) {
            timeLeft = getMillisecondsFromNumberPickers();
            isTimerRunning = true;
            Toast.makeText(this, "Meditation Started", Toast.LENGTH_LONG).show();
            Toast toastDone = Toast.makeText(this, "Meditation Finished", Toast.LENGTH_LONG);
            Toast ioErrorToast = Toast.makeText(this, "IO Exception", Toast.LENGTH_LONG);
            button.setText("Pause");
            countDownTimer = new CountDownTimer(timeLeft, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    timeLeft = millisUntilFinished;
                    timerTextView.setText(Objects.toString(new SimpleDateFormat("mm:ss").format(new Date(timeLeft))));
                }

                @Override
                public void onFinish() {
                    try {
                        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                        r.play();
                        cancelCountdownNotification();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    toastDone.show();
                    // HTTP - What. The. Fuck.
                    Thread networkThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            URL url = null;
                            HttpURLConnection urlConnection = null;
                            Log.d("info", "Eere we go");
                            try {
                                url = new URL("http://192.168.178.49:3000/meditation");
                                urlConnection = (HttpURLConnection) url.openConnection();
                                urlConnection.setRequestMethod("POST");
                                urlConnection.setRequestProperty("Content-Type", "application/json; utf-8");
                                urlConnection.setRequestProperty("Accept", "application/json");
                                urlConnection.setDoOutput(true);
                                String jsonInputString = "{\"user_id\": \"1\"}";
                                try (OutputStream os = urlConnection.getOutputStream()) {
                                    byte[] input = jsonInputString.getBytes("utf-8");
                                    os.write(input, 0, input.length);
                                } catch (Exception ex) {
                                    Log.d("info", "OutputStream Error");
                                    System.out.println(ex.toString());
                                }
                                try (BufferedReader br = new BufferedReader(
                                        new InputStreamReader(urlConnection.getInputStream(), "utf-8"))) {
                                    StringBuilder response = new StringBuilder();
                                    String responseLine = null;
                                    while ((responseLine = br.readLine()) != null) {
                                        response.append(responseLine.trim());
                                    }
                                    System.out.println(response.toString());
                                } catch (Exception ex) {
                                    Log.d("info", "ReaderError");
                                    System.out.println(ex.toString());
                                }
                            } catch (IOException ioException) {
                                ioErrorToast.show();
                            } finally {
                                Log.d("info", "Done");
                                if (urlConnection != null) urlConnection.disconnect();
                            }
                        }
                    });
                    networkThread.start();
                    // HTTP - End
                }
            }.start();

            showCountdownNotification("Meditation is ongoing...");
        } else {
            isTimerRunning = false;
            Toast.makeText(this, "Meditation Paused", Toast.LENGTH_LONG).show();
            button.setText("Start");
            countDownTimer.cancel();
            showCountdownNotification("Meditation is paused.");
        }
    }

    private void showCountdownNotification(String text) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("[Pylon] Meditation")
                .setContentText(text)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true);

        Intent notificationIntent = new Intent(this, MeditationActivity.class);
        PendingIntent notificationPendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(notificationPendingIntent);

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        manager.notify(0, notificationBuilder.build());
    }

    private void cancelCountdownNotification() {
        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        manager.cancel(0);
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void resetCountDown(View v) {
        TextView timerTextView = (TextView) findViewById(R.id.timerTextView);
        timerTextView.setText("--");
        timeLeft = getMillisecondsFromNumberPickers();
        cancelCountdownNotification();
        isTimerRunning = false;
        Button button = (Button) findViewById(R.id.meditation_start_button);
        button.setText("Start");
        countDownTimer.cancel();
    }
}