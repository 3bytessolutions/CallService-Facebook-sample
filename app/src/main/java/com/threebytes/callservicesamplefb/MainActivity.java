package com.threebytes.callservicesamplefb;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.Session;
import com.facebook.model.GraphUser;
import com.facebook.widget.FacebookDialog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.threebytes.callapi.CallService;

import java.util.Collection;

public class MainActivity extends AppCompatActivity {
    private static final int USER_HOME_ACTIVITY = 1;
    private static final int FB_LOGIN_ACTIVITY = 2;
    private static final int OUTGOING_CALL_ACTIVITY = 5;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String EXTRA_REMOTE_USER_ID = "remoteId";
    private static final String EXTRA_REMOTE_USER_NAME = "remoteName";

    /*
     * Update with your Google Cloud Project ID, visit https://console.developers.google.com/project
     */
    private static final String GOOGLE_CLOUD_PROJECT_ID = "933691616050";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!checkPlayServices())
            return;

        /*
         * check for registration
         */
        String userId = CallService.getDefaultInstance().getUserId(getApplicationContext());
        if(userId == null) {
            startFbLoginActivity();
        } else {
            startUserHomeActivity();
        }
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                finish();
            }
            return false;
        }
        return true;
    }
    private void startFbLoginActivity() {
        Intent intent = new Intent(this, FbLoginActivity.class);
        startActivityForResult(intent, FB_LOGIN_ACTIVITY);
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case USER_HOME_ACTIVITY:
                Session session = Session.getActiveSession();
                if (session != null) {
                    session.close();
                }

                getSelectedFriendData();
                break;
            case FB_LOGIN_ACTIVITY:
                if (data == null || data.getExtras() == null
                        || data.getExtras().getString("userId") == null) {
                    Toast.makeText(this, "something went wrong, try later...",
                            Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                String userId = data.getExtras().getString("userId");
                String userName = data.getExtras().getString("userName");

                if (userId == null) {
                    Toast.makeText(this, "something went wrong, try later...",
                            Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                Session.getActiveSession().close();


                final ProgressDialog progressDialog = ProgressDialog.show(this, "Registering",
                        "Please wait...", true);

                CallService.getDefaultInstance().register(userId, userName, GOOGLE_CLOUD_PROJECT_ID, MainActivity.this, new CallService.Callback() {
                    @Override
                    public void onError(Exception error) {
                        error.printStackTrace();
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "failed to register!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onSuccess() {
                        progressDialog.dismiss();

                        startUserHomeActivity();
                    }
                });

                break;
            case OUTGOING_CALL_ACTIVITY:
                startUserHomeActivity();
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }
    private void getSelectedFriendData() {
        //check if user's friends available with this app, If not than share / invite and return
        if(!UserHome.hasAppFriends){
            shareOrInvite();
            return;
        }

        Collection<GraphUser> selection = FriendPickerData.getSelectedUsers();
        if (selection != null && selection.size() > 0) {
            GraphUser friend = selection.iterator().next();

            String friendId = friend.getId();
            String friendName = friend.getName();

            Intent intent = new Intent(this, OutgoingCallActivity.class);
            intent.putExtra(EXTRA_REMOTE_USER_ID, friendId);
            intent.putExtra(EXTRA_REMOTE_USER_NAME, friendName);
            startActivityForResult(intent, OUTGOING_CALL_ACTIVITY);
        } else {
            finish();
        }
    }
    private void startUserHomeActivity() {
        Intent intent = new Intent(this, UserHome.class);

        PickFriendsActivity.populateParameters(intent, null, false, true);
        startActivityForResult(intent, USER_HOME_ACTIVITY);
    }
    private void shareOrInvite(){
        TextView frndNameView = (TextView) findViewById(R.id.textViewFrndName);
        frndNameView.setText("");
        frndNameView.setVisibility(View.VISIBLE);

        TextView errView = (TextView) findViewById(R.id.textViewErr);
        errView.setText("none of your friends have this app installed, invite them!");
        errView.setVisibility(View.VISIBLE);


        FacebookDialog.MessageDialogBuilder builder = new FacebookDialog.MessageDialogBuilder(
                MainActivity.this)
                .setLink(
                        "https://play.google.com/store/apps/details?id=com.threebytes.callservicesamplefb")
                .setName("video calling application")
                .setCaption("video calling application")
                .setPicture(
                        "https://videocallingservice.appspot.com/static/appicon.png")
                .setDescription(
                        "install this to video call me for free :-)");

        if (builder.canPresent()) {
            final FacebookDialog.MessageDialogBuilder builderFinal = builder;
            final TextView errViewFinal = errView;

            Button btn = (Button) findViewById(R.id.btnMsgYourFrnd);
            btn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    FacebookDialog dialog = builderFinal
                            .build();
                    dialog.present();
                }
            });
            btn.setVisibility(View.VISIBLE);

        } else {
            final TextView errViewFinal = errView;

            Button btn = (Button) findViewById(R.id.btnMsgYourFrnd);
            btn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent
                            .putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Hi, install this app to video call me for free : https://play.google.com/store/apps/details?id=com.threebytes.callservicesamplefb");
                    sendIntent.putExtra(Intent.EXTRA_SUBJECT,
                            "Video Call Me For FREE");
                    sendIntent.setType("text/plain");
                    startActivity(Intent.createChooser(
                            sendIntent, "Invite a friend"));
                }
            });
            btn.setVisibility(View.VISIBLE);
        }
    }
}