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
package es.ugr.swad.swadroid.modules;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import es.ugr.swad.swadroid.Constants;
import es.ugr.swad.swadroid.R;
import es.ugr.swad.swadroid.gui.DialogFactory;
import es.ugr.swad.swadroid.model.User;
import es.ugr.swad.swadroid.webservices.SOAPClient;

import org.ksoap2.serialization.SoapObject;

import java.util.ArrayList;
import java.util.Vector;

/**
 * Module for send messages.
 *
 * @author Juan Miguel Boyero Corral <juanmi1982@gmail.com>
 * @author Antonio Aguilera Malagon <aguilerin@gmail.com>
 */
public class Messages extends Module {
    /**
     * Messages tag name for Logcat
     */
    private static final String TAG = Constants.APP_TAG + " Messages";
    /**
     * Message code
     */
    private Long eventCode;
    /**
     * Message's receivers
     */
    private String receivers;
    /**
     * Names of receivers
     */
    private String receiversNames;
    /**
     * Message's subject
     */
    private String subject;
    /**
     * Message's body
     */
    private String body;
    
    private Dialog mMessageDialog;
    
    private final View.OnClickListener positiveClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
        	/*if(isDebuggable) {
	            Log.d(TAG, "notificationCode = " + Long.toString(notificationCode));
			}*/

            try {
                /*if(isDebuggable) {
                    Log.i(TAG, "selectedCourseCode = " + Long.toString(courseCode));
				}*/

                runConnection();
            } catch (Exception e) {
                String errorMsg = getString(R.string.errorServerResponseMsg);
                error(TAG, errorMsg, e, true);
            }
        }
    };
    
    private final OnClickListener negativeClickListener = new OnClickListener() {
    	@Override
        public void onClick(DialogInterface dialog, int which) {
            finish();
        }
    };
    
    private final OnCancelListener cancelClickListener = new DialogInterface.OnCancelListener() {
        public void onCancel(DialogInterface dialog) {
            setResult(RESULT_CANCELED);
            finish();
        }
    };
    
    private final OnShowListener showListener = new DialogInterface.OnShowListener() {
        @Override
        public void onShow(DialogInterface dialog) {
            EditText receiversText = (EditText) mMessageDialog.findViewById(R.id.message_receivers_text);;
            EditText subjectText = (EditText) mMessageDialog.findViewById(R.id.message_subject_text);;
            Button b = ((AlertDialog) mMessageDialog).getButton(AlertDialog.BUTTON_POSITIVE);
            
            b.setOnClickListener(positiveClickListener);
            
            if (eventCode != 0) {
                subject = getIntent().getStringExtra("summary");

                subjectText.setText("Re: " + subject);
                receiversText.setVisibility(View.GONE);
            }
        }
    };

    /* (non-Javadoc)
     * @see es.ugr.swad.swadroid.modules.Module#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        eventCode = getIntent().getLongExtra("eventCode", 0);
        mMessageDialog = DialogFactory.createPositiveNegativeDialog(this,
                                                                    R.layout.dialog_messages,
                                                                    R.string.messagesModuleLabel,
                                                                    -1,
                                                                    R.string.sendMsg,
                                                                    R.string.cancelMsg,
                                                                    // positiveClickListener,
                                                                    null,
                                                                    negativeClickListener,
                                                                    cancelClickListener);

        mMessageDialog.setOnShowListener(showListener);
        mMessageDialog.show();

        if (savedInstanceState != null) 
            writeData();

        setMETHOD_NAME("sendMessage");
        getSupportActionBar().hide();
    }

    /**
     * Reads user input from Dialog
     */
    private void readData() {
        EditText rcv = (EditText) mMessageDialog.findViewById(R.id.message_receivers_text);
        receivers = rcv.getText().toString();

        EditText subj = (EditText) mMessageDialog.findViewById(R.id.message_subject_text);
        subject = subj.getText().toString();

        EditText bd = (EditText) mMessageDialog.findViewById(R.id.message_body_text);
        body = bd.getText().toString();
    }

    /**
     * Writes user input to Dialog
     */
    private void writeData() {
        EditText rcv = (EditText) mMessageDialog.findViewById(R.id.message_receivers_text);
        rcv.setText(receivers);

        EditText subj = (EditText) mMessageDialog.findViewById(R.id.message_subject_text);
        subj.setText(subject);

        EditText bd = (EditText) mMessageDialog.findViewById(R.id.message_body_text);
        bd.setText(body);
    }

    /**
     * Adds the foot to the message body
     */
    private void addFootBody() {
        body = body.replaceAll("\n", "<br />");
        body = body + "<br /><br />" + getString(R.string.footMessageMsg) + " " + getString(R.string.app_name) +
                "<br />" + getString(R.string.marketWebURL);
        //body = body + "<br /><br />"+ getString(R.string.footMessageMsg) + " <a href=\"" +
        //		getString(R.string.marketWebURL) + "\">" + getString(R.string.app_name) + "</a>";
    }

    /* (non-Javadoc)
     * @see es.ugr.swad.swadroid.modules.Module#requestService()
     */
    @Override
    protected void requestService() throws Exception {

        readData();
        addFootBody();

        createRequest(SOAPClient.CLIENT_TYPE);
        addParam("wsKey", Constants.getLoggedUser().getWsKey());
        addParam("messageCode", eventCode.intValue());
        addParam("to", receivers);
        addParam("subject", subject);
        addParam("body", body);
        sendRequest(User.class, false);

        receiversNames = "";
        if (result != null) {
            ArrayList<?> res = new ArrayList<Object>((Vector<?>) result);
            SoapObject soap = (SoapObject) res.get(1);
            int csSize = soap.getPropertyCount();
            for (int i = 0; i < csSize; i++) {
                SoapObject pii = (SoapObject) soap.getProperty(i);
                String nickname = pii.getProperty("userNickname").toString();
                String firstname = pii.getProperty("userFirstname").toString();
                String surname1 = pii.getProperty("userSurname1").toString();
                String surname2 = pii.getProperty("userSurname2").toString();

                receiversNames += "\n";
                receiversNames += firstname + " " + surname1 + " " + surname2;

                if (!nickname.equalsIgnoreCase(Constants.NULL_VALUE) && !nickname.equalsIgnoreCase("")) {
                    receiversNames += " (" + nickname + ")";
                }
            }
        }

        setResult(RESULT_OK);
    }

    /* (non-Javadoc)
     * @see es.ugr.swad.swadroid.modules.Module#connect()
     */
    @Override
    protected void connect() {
        String progressDescription = getString(R.string.sendingMessageMsg);
        int progressTitle = R.string.messagesModuleLabel;

        startConnection(false, progressDescription, progressTitle);

        Toast.makeText(this, R.string.sendingMessageMsg, Toast.LENGTH_SHORT).show();
        Log.i(TAG, getString(R.string.sendingMessageMsg));
    }

    /* (non-Javadoc)
     * @see es.ugr.swad.swadroid.modules.Module#postConnect()
     */
    @Override
    protected void postConnect() {
        String messageSended = getString(R.string.messageSendedMsg) + ":" + receiversNames;

        Toast.makeText(this, messageSended, Toast.LENGTH_LONG).show();
        Log.i(TAG, messageSended);
        
        mMessageDialog.dismiss();

        finish();
    }

    /* (non-Javadoc)
     * @see es.ugr.swad.swadroid.modules.Module#onError()
     */
    @Override
    protected void onError() {

    }

    /* (non-Javadoc)
     * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    	eventCode = savedInstanceState.getLong("eventCode");
        receivers = savedInstanceState.getString("receivers");
        receiversNames = savedInstanceState.getString("receiversNames");
        subject = savedInstanceState.getString("subject");
        body = savedInstanceState.getString("body");

        writeData();

        super.onRestoreInstanceState(savedInstanceState);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        readData();

        outState.putLong("eventCode", eventCode);
        outState.putString("receivers", receivers);
        outState.putString("receiversNames", receiversNames);
        outState.putString("subject", subject);
        outState.putString("body", body);

        super.onSaveInstanceState(outState);
    }
}
