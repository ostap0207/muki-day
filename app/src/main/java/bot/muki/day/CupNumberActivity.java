package bot.muki.day;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.muki.core.MukiCupApi;

public class CupNumberActivity extends AppCompatActivity {

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cup_number);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Please, pour some coffee and wait...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        final TextView cupNumber = (TextView) findViewById(R.id.cupNumber);
        Button startBtn = (Button) findViewById(R.id.addCupBtn);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String cupNumberStr = cupNumber.getText().toString();
                if (cupNumberStr.isEmpty()) {
                    showToast("Please, fill in the cup id");
                } else {
                    checkPermisionAndRequest(cupNumberStr);
                }
            }
        });
    }

    private void checkPermisionAndRequest(String cupNumberStr) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CALENDAR},
                    1);
        } else {
            request(cupNumberStr);
        }
    }

    public void request(final String cupNumber) {
        showProgress();
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... strings) {
                try {
                    String serialNumber = strings[0];
                    return MukiCupApi.cupIdentifierFromSerialNumber(serialNumber);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String cupId) {
                if (cupId != null) {
                    startMainActivity(cupId);
                } else {
                    showToast("Having trouble finding your cup. Please, check the cup id and that your internet connection is on and tyr again.");
                }
            }
        }.execute(cupNumber);
    }

    private void startMainActivity(String cupId) {
        hideProgress();
        Intent intent = new Intent(CupNumberActivity.this, MainActivity.class);
        intent.putExtra("cupId", cupId);
        startActivity(intent);
    }

    private void showProgress() {
        Handler mainHandler = new Handler(this.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                progressDialog.show();
            }
        });
    }

    private void hideProgress() {
        Handler mainHandler = new Handler(this.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                progressDialog.dismiss();
            }
        });
    }

    private void showToast(final String text) {
        hideProgress();
        Toast.makeText(CupNumberActivity.this, text, Toast.LENGTH_SHORT).show();
    }
}
