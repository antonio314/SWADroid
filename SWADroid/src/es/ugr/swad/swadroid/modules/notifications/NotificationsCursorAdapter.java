/*
 *  This file is part of SWADroid.
 *
 *  Copyright (C) 2010 Juan Miguel Boyero Corral <juanmi1982@gmail.com>
 *
 *  SWADroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  SWADroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with SWADroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.ugr.swad.swadroid.modules.notifications;

import android.content.Context;
import android.database.Cursor;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import es.ugr.swad.swadroid.Constants;
import es.ugr.swad.swadroid.R;
import es.ugr.swad.swadroid.utils.Crypto;
import es.ugr.swad.swadroid.utils.Utils;

import java.util.Date;

/**
 * Custom adapter for display notifications
 *
 * @author Juan Miguel Boyero Corral <juanmi1982@gmail.com>
 */
public class NotificationsCursorAdapter extends CursorAdapter {
    private Context ctx;
    private boolean[] contentVisible;
    private String DBKey;
    private Crypto crypto;

    /**
     * Constructor
     *
     * @param context Application context
     * @param c       Database cursor
     */
    public NotificationsCursorAdapter(Context context, Cursor c) {
        super(context, c, true);

        ctx = context;
        int numRows = c.getCount();

        contentVisible = new boolean[numRows];
        for (int i = 0; i < numRows; i++) {
            contentVisible[i] = false;
        }
    }

    /**
     * Constructor
     *
     * @param context Application context
     * @param c       Database cursor
     * @param key     Database key
     */
    public NotificationsCursorAdapter(Context context, Cursor c, String key) {
        super(context, c, true);

        DBKey = key;
        crypto = new Crypto(DBKey);
        ctx = context;
        int numRows = c.getCount();

        contentVisible = new boolean[numRows];
        for (int i = 0; i < numRows; i++) {
            contentVisible[i] = false;
        }
    }

    /**
     * Constructor
     *
     * @param context     Application context
     * @param c           Database cursor
     * @param autoRequery Flag to set autoRequery function
     */
    public NotificationsCursorAdapter(Context context, Cursor c,
                                      boolean autoRequery) {

        super(context, c, autoRequery);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final Long notifCode = cursor.getLong(cursor.getColumnIndex("notifCode"));
        final Long eventCode = cursor.getLong(cursor.getColumnIndex("eventCode"));
        final String userPhoto = cursor.getString(cursor.getColumnIndex("userPhoto"));
        long unixTime;
        String type = "";
        String sender, senderFirstname, senderSurname1, senderSurname2, summaryText;
        String contentText, contentMsgText;
        Date d;
        java.text.DateFormat dateShortFormat = android.text.format.DateFormat.getDateFormat(context);
        java.text.DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);
        int numRows = cursor.getCount();
        int cursorPosition = cursor.getPosition();
        String seenLocalString = cursor.getString(cursor.getColumnIndex("seenLocal"));
        boolean seenLocal = (seenLocalString == null)? false : Utils.parseStringBool(seenLocalString);
        
        if(!seenLocal) {
        	view.setBackgroundColor(context.getResources().getColor(R.color.notifications_background_highlighted));
        } else {
        	view.setBackgroundColor(context.getResources().getColor(R.color.background));
        }

        if (contentVisible.length == 0) {
            contentVisible = new boolean[numRows];
        }

        view.setScrollContainer(false);
        TextView notifCodeHided = (TextView) view.findViewById(R.id.notifCode);
        TextView eventCodeHided = (TextView) view.findViewById(R.id.eventCode);
        TextView seenLocalHided = (TextView) view.findViewById(R.id.seenLocal);
        TextView eventUserPhoto = (TextView) view.findViewById(R.id.eventUserPhoto);
        TextView eventType = (TextView) view.findViewById(R.id.eventType);
        TextView eventDate = (TextView) view.findViewById(R.id.eventDate);
        TextView eventTime = (TextView) view.findViewById(R.id.eventTime);
        TextView eventSender = (TextView) view.findViewById(R.id.eventSender);
        TextView location = (TextView) view.findViewById(R.id.eventLocation);
        final TextView summary = (TextView) view.findViewById(R.id.eventSummary);
        TextView content = (TextView) view.findViewById(R.id.eventText);
        TextView contentMsg = (TextView) view.findViewById(R.id.eventMsg);
        ImageView notificationIcon = (ImageView) view.findViewById(R.id.notificationIcon);

        if (eventType != null) {
            notifCodeHided.setText(notifCode.toString());
            eventCodeHided.setText(eventCode.toString());
            seenLocalHided.setText(seenLocalString);
            eventUserPhoto.setText(crypto.decrypt(userPhoto));
            type = crypto.decrypt(cursor.getString(cursor.getColumnIndex("eventType")));

            if (type.equals("examAnnouncement")) {
                type = context.getString(R.string.examAnnouncement);
                notificationIcon.setImageResource(R.drawable.announce);
            } else if (type.equals("marksFile")) {
                type = context.getString(R.string.marksFile);
                notificationIcon.setImageResource(R.drawable.grades);
            } else if (type.equals("notice")) {
                type = context.getString(R.string.notice);
                notificationIcon.setImageResource(R.drawable.note);
            } else if (type.equals("message")) {
                type = context.getString(R.string.message);
                notificationIcon.setImageResource(R.drawable.msg_received);
            } else if (type.equals("forumPostCourse")) {
                type = context.getString(R.string.forumPostCourse);
                notificationIcon.setImageResource(R.drawable.forum);
            } else if (type.equals("forumReply")) {
                type = context.getString(R.string.forumReply);
                notificationIcon.setImageResource(R.drawable.forum);
            } else if (type.equals("assignment")) {
                type = context.getString(R.string.assignment);
                notificationIcon.setImageResource(R.drawable.desk);
            } else if (type.equals("documentFile")) {
                type = context.getString(R.string.documentFile);
                notificationIcon.setImageResource(R.drawable.file);
            } else if (type.equals("sharedFile")) {
                type = context.getString(R.string.sharedFile);
                notificationIcon.setImageResource(R.drawable.file);
            } else if (type.equals("enrollment")) {
                type = context.getString(R.string.enrollment);
                notificationIcon.setImageResource(R.drawable.enrollment);
            } else if (type.equals("enrollmentRequest")) {
                type = context.getString(R.string.enrollmentRequest);
                notificationIcon.setImageResource(R.drawable.enrollment_request);
            } else if (type.equals("documentFile")) {
                type = context.getString(R.string.survey);
                notificationIcon.setImageResource(R.drawable.survey);
            } else {
                type = context.getString(R.string.unknownNotification);
                notificationIcon.setImageResource(R.drawable.ic_launcher_swadroid);
            }

            eventType.setText(type);
        }
        if ((eventDate != null) && (eventTime != null)) {
            unixTime = Long.parseLong(cursor.getString(cursor.getColumnIndex("eventTime")));
            d = new Date(unixTime * 1000);
            eventDate.setText(dateShortFormat.format(d));
            eventTime.setText(timeFormat.format(d));
        }
        if (eventSender != null) {
            sender = "";
            senderFirstname = crypto.decrypt(cursor.getString(cursor.getColumnIndex("userFirstname")));
            senderSurname1 = crypto.decrypt(cursor.getString(cursor.getColumnIndex("userSurname1")));
            senderSurname2 = crypto.decrypt(cursor.getString(cursor.getColumnIndex("userSurname2")));

            //Empty fields checking
            if (!senderFirstname.equals(Constants.NULL_VALUE))
                sender += senderFirstname + " ";
            if (!senderSurname1.equals(Constants.NULL_VALUE))
                sender += senderSurname1 + " ";
            if (!senderSurname2.equals(Constants.NULL_VALUE))
                sender += senderSurname2;

            eventSender.setText(sender);
        }
        if (location != null) {
            location.setText(Html.fromHtml(crypto.decrypt(cursor.getString(cursor.getColumnIndex("location")))));
        }
        if (summary != null) {
            summaryText = crypto.decrypt(cursor.getString(cursor.getColumnIndex("summary")));

            //Empty field checking
            if (summaryText.equals(Constants.NULL_VALUE))
                summaryText = context.getString(R.string.noSubjectMsg);

            summary.setText(Html.fromHtml(summaryText));
        }
        if ((content != null)) {
            contentText = crypto.decrypt(cursor.getString(cursor.getColumnIndex("content")));

            //Empty field checking
            if (contentText.equals(Constants.NULL_VALUE))
                contentText = context.getString(R.string.noContentMsg);

            content.setText(contentText);

            if (type.equals(context.getString(R.string.marksFile))) {
                contentMsgText = context.getString(R.string.marksMsg);
                contentMsg.setText(contentMsgText);

                if (cursorPosition < contentVisible.length) {
                    contentVisible[cursorPosition] = true;
                }
            } else {
                contentMsgText = "";
                contentMsg.setText(contentMsgText);

                if (cursorPosition < contentVisible.length) {
                    contentVisible[cursorPosition] = false;
                }
            }

            if ((cursorPosition < contentVisible.length) && contentVisible[cursorPosition]) {
                contentMsg.setVisibility(View.VISIBLE);
            } else {
                contentMsg.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater vi = LayoutInflater.from(context);
        return vi.inflate(R.layout.list_item_notifications, parent, false);
    }
}
