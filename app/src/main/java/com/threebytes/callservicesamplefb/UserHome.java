package com.threebytes.callservicesamplefb;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.facebook.widget.FacebookDialog;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UserHome extends ActionBarActivity {
	private static final int PICK_FRIENDS_ACTIVITY = 1;
	FacebookDialog.MessageDialogBuilder builder;
    private UiLifecycleHelper lifecycleHelper;
    private Bundle savedInstanceState;
    public static boolean hasAppFriends = true;
    private static final List<String> PERMISSIONS = new ArrayList<String>() {
        {
            add("user_friends");
            add("public_profile");
        }
    };

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.user_home);

		ActionBar actionBar = getSupportActionBar();
		actionBar.hide();

        FriendPickerData.setSelectedUsers(null);

		builder = new FacebookDialog.MessageDialogBuilder(UserHome.this)
				.setLink(
						"https://play.google.com/store/apps/details?id=com.threebytes.callservicesamplefb")
				.setName("video calling application")
				.setCaption("video calling application")
				.setPicture(
						"https://videocallingservice.appspot.com/static/appicon.png")
				.setDescription("install this to video call me for free :-)");
		//check here for any friends with this app

        initializeFBSession();
        populateContact();

        TextView legalsView = (TextView) findViewById(R.id.textLegals);
        legalsView.setMovementMethod(LinkMovementMethod.getInstance());

    }

	public void inviteFriends(View view) {
		// If the Facebook Messenger app is installed and we can present
		// the share dialog
		if (builder.canPresent()) {
			FacebookDialog dialog = builder.build();
			dialog.present();
		} else {
			// show Sharing options
			Intent sendIntent = new Intent();
			sendIntent.setAction(Intent.ACTION_SEND);
			sendIntent
					.putExtra(
							Intent.EXTRA_TEXT,
							"Hi, install this app to video call me for free : https://play.google.com/store/apps/details?id=com.threebytes.callservicesamplefb");
			sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Video Call Me For FREE");
			sendIntent.setType("text/plain");
			startActivity(Intent.createChooser(sendIntent, "Invite a friend"));
		}
	}

	public void pickFriend(View view) {
        if(hasAppFriends){
            Intent intent = new Intent(this, PickFriendsActivity.class);

            PickFriendsActivity.populateParameters(intent, null, false, true);
            startActivityForResult(intent, PICK_FRIENDS_ACTIVITY);
        }else {
            setResult(RESULT_OK, null);
            finish();
        }
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case PICK_FRIENDS_ACTIVITY:
            /*
			Session session = Session.getActiveSession();
			if(session != null) {
				session.close();
			}
			*/
			
			Collection<GraphUser> selection = FriendPickerData.getSelectedUsers();
			if (selection != null && selection.size() > 0) {
				finish();
			}
		}
	}

	@Override
	public void onBackPressed() {
        hasAppFriends = true;
		finish();
	}

    public void populateContact() {
        final Session session = Session.getActiveSession();
        new Request(session, "/me/friends", null, HttpMethod.GET,
                new Request.Callback() {
                    public void onCompleted(Response response) {

                        // Process the returned response
                        GraphObject graphObject = response.getGraphObject();
                        if (graphObject != null) {
                            // Check if there is extra data
                            if (graphObject.getProperty("data") != null) {
                                    JSONArray arrayObject = (JSONArray) graphObject
                                            .getProperty("data");

                                    int count = arrayObject.length();
                                    if(count == 0)
                                        hasAppFriends = false;
                                    // Ensure the user has at least one friend
                                    //session.close();
                            }
                        }
                    }

                }).executeAsync();
    }

    private void initializeFBSession() {
        lifecycleHelper = new UiLifecycleHelper(this,
                new Session.StatusCallback() {
                    @Override
                    public void call(Session session, SessionState state,
                                     Exception exception) {
                        onSessionStateChanged(session, state, exception);
                    }
                });
        lifecycleHelper.onCreate(savedInstanceState);

        ensureOpenSession();

    }
    private boolean ensureOpenSession() {
        if (Session.getActiveSession() == null
                || !Session.getActiveSession().isOpened()) {
            Session.openActiveSession(this, true, PERMISSIONS,
                    new Session.StatusCallback() {
                        @Override
                        public void call(Session session, SessionState state,
                                         Exception exception) {
                            onSessionStateChanged(session, state, exception);
                        }
                    });
            return false;
        }
        // friendPickerFragment.loadData(false);

        return true;
    }

    private void onSessionStateChanged(final Session session,
                                       SessionState state, Exception exception) {
        if (state.isOpened() && !sessionHasNecessaryPerms(session)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.need_perms_alert_text);
            builder.setPositiveButton(R.string.need_perms_alert_button_ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            session.requestNewReadPermissions(new Session.NewPermissionsRequest(
                                    UserHome.this, getMissingPermissions(session)));
                        }
                    });
            builder.setNegativeButton(R.string.need_perms_alert_button_quit,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
            builder.show();
        } else if (state.isOpened()) {
            // friendPickerFragment.loadData(false);
        }
    }

    private boolean sessionHasNecessaryPerms(Session session) {
        if (session != null && session.getPermissions() != null) {
            for (String requestedPerm : PERMISSIONS) {
                if (!session.getPermissions().contains(requestedPerm)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private List<String> getMissingPermissions(Session session) {
        List<String> missingPerms = new ArrayList<String>(PERMISSIONS);
        if (session != null && session.getPermissions() != null) {
            for (String requestedPerm : PERMISSIONS) {
                if (session.getPermissions().contains(requestedPerm)) {
                    missingPerms.remove(requestedPerm);
                }
            }
        }
        return missingPerms;
    }
}
