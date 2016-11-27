package bot.muki.day;

import java.util.Date;

/*
 * Created by David Laundav and contributed by Christian Orthmann
 *
 * Copyright 2013 David Laundav
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

public class CalendarEvent implements Comparable<CalendarEvent>{

    private String title;
    private String location;
    private Date begin, end;
    private boolean allDay;

    public CalendarEvent() {

    }

    public CalendarEvent(String title, Date begin, Date end, boolean allDay, String location) {
        setTitle(title);
        setBegin(begin);
        setEnd(end);
        setAllDay(allDay);
        setLocation(location);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Date getBegin() {
        return begin;
    }

    public void setBegin(Date begin) {
        this.begin = begin;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public boolean isAllDay() {
        return allDay;
    }

    public void setAllDay(boolean allDay) {
        this.allDay = allDay;
    }

    @Override
    public String toString(){
        return getTitle() + " " + getBegin() + " " + getEnd() + " " + isAllDay() + " " + getLocation();
    }

    @Override
    public int compareTo(CalendarEvent other) {
        // -1 = less, 0 = equal, 1 = greater
        return getBegin().compareTo(other.begin);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CalendarEvent that = (CalendarEvent) o;

        if (allDay != that.allDay) return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        if (location != null ? !location.equals(that.location) : that.location != null)
            return false;
        if (begin != null ? !begin.equals(that.begin) : that.begin != null) return false;
        return end != null ? end.equals(that.end) : that.end == null;

    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (location != null ? location.hashCode() : 0);
        result = 31 * result + (begin != null ? begin.hashCode() : 0);
        result = 31 * result + (end != null ? end.hashCode() : 0);
        result = 31 * result + (allDay ? 1 : 0);
        return result;
    }
}