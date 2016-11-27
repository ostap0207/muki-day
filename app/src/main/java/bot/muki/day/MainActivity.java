package bot.muki.day;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.muki.core.model.ImageProperties;
import com.muki.core.util.ImageUtils;

import java.text.DateFormat;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ImageView mCupImage;
    private SeekBar mContrastSeekBar;
    private ProgressDialog progressDialog;

    private Bitmap mImage;
    private int mContrast = ImageProperties.DEFAULT_CONTRACT;

    private String mCupId;
    Bitmap calendarBitmap;
    private String selectedCalendar;
    private Intent serviceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCupId = getIntent().getStringExtra("cupId");

        final List<String> calendars = CalendarService.getCalendars(this);
        selectedCalendar  = calendars.get(0);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Please, pour some coffee and wait...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        mCupImage = (ImageView) findViewById(R.id.imageSrc);
        mContrastSeekBar = (SeekBar) findViewById(R.id.contrastSeekBar);
        mContrastSeekBar.setProgress(100);
        mContrastSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mContrast = i - 100;
                showProgress();
                setupImage();
                startSender(mCupId);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        Spinner calendarList = (Spinner) findViewById(R.id.calendars);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, calendars);
        calendarList.setAdapter(arrayAdapter);
        calendarList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedCalendar = calendars.get(i);
                updateBitmap(selectedCalendar);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        updateBitmap(selectedCalendar);

        Button startBtn = (Button) findViewById(R.id.startBtn);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSender(mCupId);
                Toast.makeText(MainActivity.this, "We are all set now!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateBitmap(String selectedCalendar) {
        List<CalendarEvent> events = CalendarService.readCalendar(this, selectedCalendar);
        DateFormat dateFormat = android.text.format.DateFormat.getTimeFormat(getApplicationContext());
        calendarBitmap = drawEvents(this, events, dateFormat);
        reset();
    }

    private void setupImage() {
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... voids) {
                Bitmap result = Bitmap.createBitmap(mImage);
                ImageUtils.convertImageToCupImage(result, mContrast);
                return result;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                mCupImage.setImageBitmap(bitmap);
                hideProgress();
            }
        }.execute();
    }

    public void reset() {
        showProgress();
        mImage = ImageUtils.scaleBitmapToCupSize(calendarBitmap);
        mContrast = ImageProperties.DEFAULT_CONTRACT;
        mContrastSeekBar.setProgress(100);
        setupImage();
    }

    public Bitmap drawEvents(Context gContext,
                                   List<CalendarEvent> events, DateFormat dateFormat) {
        Resources resources = gContext.getResources();
        float scale = resources.getDisplayMetrics().density;
        int w = 300, h = 600;

        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = Bitmap.createBitmap(w, h, conf);

        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        Paint primary = new Paint(Paint.ANTI_ALIAS_FLAG);
        primary.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        primary.setColor(Color.BLACK);
        primary.setTextSize((int) (14 * scale));
        primary.setShadowLayer(1f, 0f, 1f, Color.BLACK);

        Paint secondary = new Paint(Paint.ANTI_ALIAS_FLAG);
        secondary.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        secondary.setColor(Color.BLACK);
        secondary.setTextSize((int) (10 * scale));
        secondary.setShadowLayer(1f, 0f, 1f, Color.BLACK);

        Bitmap clockIcon = scaleDown(((BitmapDrawable) getResources().getDrawable(R.drawable.clock)).getBitmap(), 20, false);
        Bitmap locationIcon = scaleDown(((BitmapDrawable) getResources().getDrawable(R.drawable.location)).getBitmap(), 20, false);

        int lastY = 30;
        for (CalendarEvent event : events) {
            int lastX = 0;
            canvas.drawText(event.getTitle(), 0, lastY, primary);
            canvas.drawBitmap(clockIcon, lastX, lastY + 30, primary);
            lastX += 20;
            Rect bounds = new Rect();
            String time;
            if (event.isAllDay()) {
                time = "All day";
            } else {
                time = dateFormat.format(event.getBegin());
            }
            secondary.getTextBounds(time, 0, time.length(), bounds);
            canvas.drawText(time, lastX, lastY + 50, secondary);
            lastX += bounds.width();
            lastX += 10;


            String location = getLocation(event);
            if (!location.isEmpty()) {
                canvas.drawBitmap(locationIcon, lastX, lastY + 30, primary);
                lastX += 20;
            }
            canvas.drawText(location, lastX, lastY + 50, secondary);
            canvas.drawLine(0, lastY + 60, w, lastY + 60, primary);
            lastY += 100;
        }

        return bitmap;
    }

    public static Bitmap scaleDown(Bitmap realImage, float maxImageSize,
                                   boolean filter) {
        float ratio = Math.min(
                (float) maxImageSize / realImage.getWidth(),
                (float) maxImageSize / realImage.getHeight());
        int width = Math.round((float) ratio * realImage.getWidth());
        int height = Math.round((float) ratio * realImage.getHeight());

        return Bitmap.createScaledBitmap(realImage, width,
                height, filter);
    }

    private String getLocation(CalendarEvent event) {
        if (!event.getLocation().isEmpty())
            return event.getLocation();
        else return "";
    }

    private void startSender(String cupId) {
        if (serviceIntent != null) {
            stopService(serviceIntent);
        }
        serviceIntent = new Intent(MainActivity.this, MukiService.class);
        serviceIntent.putExtra("calendarName", selectedCalendar);
        serviceIntent.putExtra("cupId", cupId);
        serviceIntent.putExtra("contrast", mContrast);
        startService(serviceIntent);
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
}
