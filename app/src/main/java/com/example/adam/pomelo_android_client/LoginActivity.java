package com.example.adam.pomelo_android_client;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.zvidia.pomelo.exception.PomeloException;
import com.zvidia.pomelo.protocol.PomeloMessage;
import com.zvidia.pomelo.websocket.OnDataHandler;
import com.zvidia.pomelo.websocket.OnErrorHandler;
import com.zvidia.pomelo.websocket.OnHandshakeSuccessHandler;
import com.zvidia.pomelo.websocket.PomeloClient;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {
    String LOG_TAG = getClass().getSimpleName();

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };

    private final String number = "" + new Date().getTime();

    // UI references.
    private AutoCompleteTextView m_username;
    private EditText m_password;

    private View mProgressView;
    private View mLoginFormView;

    String host = null;
    String port = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        m_username = (AutoCompleteTextView) findViewById(R.id.username);
        populateAutoComplete();

        m_password = (EditText) findViewById(R.id.password);
        m_password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.login_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(m_username, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {

        // Reset errors.
        m_username.setError(null);
        m_password.setError(null);

        // Store values at the time of the login attempt.
        String email = m_username.getText().toString();
        String password = m_password.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            m_password.setError(getString(R.string.error_field_required));
            focusView = m_password;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            m_username.setError(getString(R.string.error_field_required));
            focusView = m_username;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            //showProgress(true);
            doLogin();
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        m_username.setAdapter(adapter);
    }


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    OnDataHandler onEventHandler = new OnDataHandler() {
        @Override
        public void onData(PomeloMessage.Message message) {
            Log.d(LOG_TAG, message.toString());
            JSONObject msgJson = message.getBodyJson();
            try {
            switch (message.getRoute()){
                case "onCall": {
                    Log.i(LOG_TAG, "a call is made from:" + msgJson.getString("from") + " to:" + msgJson.getString("to"));
                    break;
                }
                case "onLeave":{
                    Log.i(LOG_TAG, "a user is left:" + msgJson.getString("user"));
                    break;
                }
                case "onAdd":{
                    Log.i(LOG_TAG, "a user is add:" + msgJson.getString("user"));
                    break;
                }
            }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    OnHandshakeSuccessHandler onConnectorHandshakeSuccessHandler = new OnHandshakeSuccessHandler() {
        @Override
        public void onSuccess(final PomeloClient client, JSONObject jsonObject) {
            try {
                JSONObject connectorJson = new JSONObject();
                String username = m_username.getText().toString();
                String password = m_password.getText().toString();


                String uid = username + "<-->mobile<-->" + number;

                connectorJson.put("uid", uid);
                connectorJson.put("password", password);

                client.setOnEventHandler(onEventHandler);

                client.request("connector.entryHandler.enter", connectorJson.toString(), new OnDataHandler() {
                    @Override
                    public void onData(PomeloMessage.Message message) {
                        try {
                            JSONObject msgJson = message.getBodyJson();
                            if(msgJson.getInt("code") == 200){
                                Log.i(LOG_TAG, "enter romm success!");

                                try {
                                    JSONObject callStateJson = new JSONObject();
                                    callStateJson.put("from", number);
                                    callStateJson.put("state","IDLE"); //OFFHOOK, RING
                                    callStateJson.put("number", "testIncomingNumber");
                                    client.request("chat.chatHandler.changeCallState", callStateJson.toString(), null);
                                } catch (PomeloException e) {
                                    e.printStackTrace();
                                }

                                try {
                                    JSONObject recordJson = new JSONObject();
                                    recordJson.put("from", number);
                                    recordJson.put("to","testToNumber"); //OFFHOOK, RING
                                    recordJson.put("url", "http://testRecordUrl/test.mp3");
                                    client.request("chat.chatHandler.sendRecordUrl", recordJson.toString(), null);
                                } catch (PomeloException e) {
                                    e.printStackTrace();
                                }
                            }else{
                                Log.e(LOG_TAG, "enter failed!");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
            } catch (PomeloException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
            }
        }
    };

    OnHandshakeSuccessHandler onGateHandshakeSuccessHandler = new OnHandshakeSuccessHandler() {
        @Override
        public void onSuccess(final PomeloClient client, JSONObject jsonObject) {
            Log.d(LOG_TAG, "on gate handshake success!" + jsonObject.toString());
            try {
                JSONObject gateJson = new JSONObject();
                String username = m_username.getText().toString();
                String channel = m_password.getText().toString();
                gateJson.put("uid", username);
                client.request("gate.gateHandler.queryEntry", gateJson.toString(), new OnDataHandler() {
                    @Override
                    public void onData(PomeloMessage.Message message) {
                        JSONObject bodyJson = message.getBodyJson();
                        int code = 0;
                        try {
                            code = bodyJson.getInt(PomeloClient.HANDSHAKE_RES_CODE_KEY);
                            if (code == 200) {
                                host = bodyJson.getString(PomeloClient.HANDSHAKE_RES_HOST_KEY);
                                port = bodyJson.getString(PomeloClient.HANDSHAKE_RES_PORT_KEY);
                                client.close();

                                //connect to the connector server
                                PomeloClient connector = new PomeloClient(new URI("ws://" + host + ":" + port));
                                connector.setOnHandshakeSuccessHandler(onConnectorHandshakeSuccessHandler);
                                connector.setOnErrorHandler(onErrorHandler);
                                connector.connect();
                            }
                        } catch (JSONException e) {
                            Log.e(LOG_TAG, e.getMessage(), e);
                        } catch (URISyntaxException e) {
                            Log.e(LOG_TAG, e.getMessage(), e);
                        }
                    }
                });
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
            } catch (PomeloException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
            }
        }
    };

    OnErrorHandler onErrorHandler = new OnErrorHandler() {
        @Override
        public void onError(Exception e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
    };

    public void doLogin() {
        try {
            Log.d(LOG_TAG, "start connector to pomelo server");
            PomeloClient gateConnector = new PomeloClient(new URI("ws://117.25.156.237:3014"));
            gateConnector.setOnHandshakeSuccessHandler(onGateHandshakeSuccessHandler);
            gateConnector.setOnErrorHandler(onErrorHandler);
            gateConnector.connect();
        } catch (URISyntaxException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
    }
}

