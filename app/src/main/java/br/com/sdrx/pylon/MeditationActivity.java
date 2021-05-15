package br.com.sdrx.pylon;

        import androidx.appcompat.app.AppCompatActivity;

        import android.media.Ringtone;
        import android.media.RingtoneManager;
        import android.net.Uri;
        import android.os.Bundle;
        import android.os.CountDownTimer;
        import android.util.Log;
        import android.view.View;
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

    private CountDownTimer countDownTimer;
    private long timeLeft = 1 * 10 * 1000; // 1 minute
    private boolean isTimerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meditation);
        TextView timerTextView = (TextView) findViewById(R.id.timerTextView);
        timerTextView.setText(Objects.toString(new SimpleDateFormat("mm:ss").format(new Date(timeLeft))));
    }

    public void startMeditate(View view) {
        TextView timerTextView = (TextView) findViewById(R.id.timerTextView);
        Button button = (Button) view;
        if (!isTimerRunning) {
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
        } else {
            isTimerRunning = false;
            Toast.makeText(this, "Meditation Paused", Toast.LENGTH_LONG).show();
            button.setText("Start");
            countDownTimer.cancel();
        }
    }
}