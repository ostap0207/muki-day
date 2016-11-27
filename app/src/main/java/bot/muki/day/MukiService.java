package bot.muki.day;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.muki.core.MukiCupApi;
import com.muki.core.MukiCupCallback;
import com.muki.core.model.Action;
import com.muki.core.model.DeviceInfo;
import com.muki.core.model.ErrorCode;
import com.muki.core.model.ImageProperties;
import com.muki.core.util.ImageUtils;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MukiService extends Service{

    private int WAIT_MINUTES = 30;

    private MukiCupApi mMukiCupApi;
    private String calendarName;
    private String cupId;
    private int contrast;
    private Date lastSent;
    private List<CalendarEvent> lastEvents;
    private Timer timer;
    private boolean cupConnected;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("Service started");
        calendarName = intent.getStringExtra("calendarName");
        contrast = intent.getIntExtra("contrast", ImageProperties.DEFAULT_CONTRACT);
        cupId = intent.getStringExtra("cupId");

        mMukiCupApi = new MukiCupApi(getApplicationContext(), new MukiCupCallback() {
            @Override
            public void onCupConnected() {
                System.out.println("Cup connected");
                cupConnected = true;
            }

            @Override
            public void onCupDisconnected() {
                cupConnected = false;
            }

            @Override
            public void onDeviceInfo(DeviceInfo deviceInfo) {
            }

            @Override
            public void onImageCleared() {
            }

            @Override
            public void onImageSent() {
                System.out.println("Image sent");
                lastSent = new Date();
            }

            @Override
            public void onError(Action action, ErrorCode errorCode) {
                if (action == Action.SEND_IMAGE && errorCode == ErrorCode.UNKNOWN_ERROR) {
                    lastSent = new Date();
                }
            }
        });

        startTimer();
        return Service.START_REDELIVER_INTENT;
    }

    private void startTimer() {
        System.out.println("Timer started");
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                List<CalendarEvent> events = CalendarService.readCalendar(MukiService.this, calendarName);
                if (lastEvents == null || !lastEvents.equals(events) || timePassed()) {
                    System.out.println("Send");
                    if (lastEvents != null) {
                        System.out.println(!lastEvents.equals(events));
                    }
                    lastEvents = events;
                    send(events, cupId);
                }
            }
        },0 , 20000);
    }

    private boolean timePassed() {
        Date currentDate = new Date();
        if (lastSent == null)
            return true;
        return (currentDate.getTime() - lastSent.getTime()) / (60 * 1000) > 30;
    }

    private void send(List<CalendarEvent> events, String cupId) {
        DateFormat dateFormat = android.text.format.DateFormat.getTimeFormat(getApplicationContext());
        Bitmap calendarBitmap = drawEvents(this, events, dateFormat);
        Bitmap resizedImage = ImageUtils.scaleBitmapToCupSize(calendarBitmap);
        mMukiCupApi.sendImage(resizedImage, new ImageProperties(contrast), cupId);
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
        secondary.setTextSize((int) (12 * scale));
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

        Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width,
                height, filter);
        return newBitmap;
    }

    private String getLocation(CalendarEvent event) {
        if (!event.getLocation().isEmpty())
            return event.getLocation();
        else return "";
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean stopService(Intent name) {
        timer.cancel();
        return super.stopService(name);
    }
}
