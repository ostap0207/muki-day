package bot.muki.day;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.support.v4.app.ActivityCompat;
import android.text.format.DateUtils;

/*
 * Created by David Laundav and contributed by Christian Orthmann
 *
 * Copyright 2013 Daivd Laundav
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * References:
 * http://stackoverflow.com/questions/5883938/getting-events-from-calendar
 *
 * Please do not delete the references as they gave inspiration for the implementation
 */


public class CalendarService {


    public static List<String> getCalendars(Context context) {
        ContentResolver contentResolver = context.getContentResolver();

        Uri uri = CalendarContract.Calendars.CONTENT_URI;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return new ArrayList<>();
        }

        String[] EVENT_PROJECTION = new String[]{
                CalendarContract.Calendars._ID,                           // 0
                CalendarContract.Calendars.ACCOUNT_NAME,                  // 1
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,         // 2
                CalendarContract.Calendars.OWNER_ACCOUNT                  // 3
        };

        Cursor cursor = contentResolver.query(uri, EVENT_PROJECTION,
                null, null, null);

        ArrayList<String> calendars = new ArrayList<>();
        while (cursor.moveToNext()) {
            calendars.add(cursor.getString(2));
            System.out.println(cursor.getString(2));
        }

        return calendars;
    }

    // Default constructor
    public static List<CalendarEvent> readCalendar(Context context, String calendarName) {
        return readCalendar(context, 1, 0, calendarName);
    }

    // Use to specify specific the time span
    public static List<CalendarEvent> readCalendar(Context context, int days, int hours, String calendarName) {

        ContentResolver contentResolver = context.getContentResolver();


        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return new ArrayList<>();
        }
        Cursor cursor = contentResolver.query(CalendarContract.Events.CONTENT_URI,
                new String[]{CalendarContract.Calendars._ID, "calendar_displayName", "title", "description", "dtstart", "dtend", "eventLocation"},
                null, null, null);

        // Create a set containing all of the calendar IDs available on the phone
        HashSet<String> calendarIds = getCalenderIds(cursor);

        // Create a hash map of calendar ids and the events of each id
        HashMap<String, List<CalendarEvent>> eventMap = new HashMap<String, List<CalendarEvent>>();

            // Create a builder to define the time span
            Uri.Builder builder = Uri.parse("content://com.android.calendar/instances/when").buildUpon();
            long now = new Date().getTime();

            // create the time span based on the inputs
            ContentUris.appendId(builder, now);
            ContentUris.appendId(builder, now + (DateUtils.DAY_IN_MILLIS * days) + (DateUtils.HOUR_IN_MILLIS * hours));

            // Create an event cursor to find all events in the calendar
            Cursor eventCursor = contentResolver.query(builder.build(),
                    new String[]  { "title", "begin", "end", "allDay", "eventLocation"}, "calendar_displayName='" + calendarName + "'",
                    null, "startDay ASC, startMinute ASC");

//            System.out.println("eventCursor count="+eventCursor.getCount());

            // If there are actual events in the current calendar, the count will exceed zero
            if(eventCursor.getCount()>0) {

                // Create a list of calendar events for the specific calendar
                List<CalendarEvent> eventList = new ArrayList<CalendarEvent>();

                // Move to the first object
                eventCursor.moveToFirst();

                // Create an object of CalendarEvent which contains the title, when the event begins and ends,
                // and if it is a full day event or not
                CalendarEvent ce = loadEvent(eventCursor);

                // Adds the first object to the list of events
                eventList.add(ce);

                System.out.println(ce.toString());

                // While there are more events in the current calendar, move to the next instance
                while (eventCursor.moveToNext()) {

                    // Adds the object to the list of events
                    ce = loadEvent(eventCursor);
                    eventList.add(ce);

                    System.out.println(ce.toString());

                }

                Collections.sort(eventList);

//                System.out.println(eventList);
                return eventList;
            }
        return new ArrayList<>();
    }

    private static CalendarEvent loadEvent(Cursor csr) {
        return new CalendarEvent(csr.getString(0),
                new Date(csr.getLong(1)),
                new Date(csr.getLong(2)),
                !csr.getString(3).equals("0"),
                csr.getString(4)
                );
    }

    private static HashSet<String> getCalenderIds(Cursor cursor) {
        HashSet<String> calendarIds = new HashSet<String>();
        try {
            if(cursor.getCount() > 0) {
                while (cursor.moveToNext()) {

                    String _id = cursor.getString(1);
                    calendarIds.add(_id);
                }
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }

        return calendarIds;
    }
}