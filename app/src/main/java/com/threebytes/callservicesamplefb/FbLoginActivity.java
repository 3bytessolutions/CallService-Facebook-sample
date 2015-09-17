package com.threebytes.callservicesamplefb;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.NewPermissionsRequest;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;

import java.util.ArrayList;
import java.util.List;

public class FbLoginActivity extends ActionBarActivity {
	@SuppressWarnings("serial")
	private static final List<String> PERMISSIONS = new ArrayList<String>() {
		{
			add("user_friends");
			add("public_profile");
		}
	};

	private UiLifecycleHelper lifecycleHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.fb_login);

		final Button button = (Button) findViewById(R.id.btnContinue);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				((TextView) findViewById(R.id.textLoginDesc)).setText("");
				Button btnContinue = (Button) findViewById(R.id.btnContinue);
				btnContinue.setText("Please wait...");
				btnContinue.setEnabled(false);

				onClickContinue();
			}
		});

		lifecycleHelper = new UiLifecycleHelper(this,
				new Session.StatusCallback() {
					@Override
					public void call(Session session, SessionState state,
							Exception exception) {
						onSessionStateChanged(session, state, exception);
					}
				});
		lifecycleHelper.onCreate(savedInstanceState);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Session.getActiveSession().onActivityResult(this, requestCode,
				resultCode, data);
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
		makeMeRequest();
		return true;
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

	private void onSessionStateChanged(final Session session,
			SessionState state, Exception exception) {
		if (state.isOpened() && !sessionHasNecessaryPerms(session)) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.need_perms_alert_text);
			builder.setPositiveButton(R.string.need_perms_alert_button_ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							session.requestNewReadPermissions(new NewPermissionsRequest(
									FbLoginActivity.this,
									getMissingPermissions(session)));
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
			makeMeRequest();
		}
	}

	private void makeMeRequest() {
		final Session session = Session.getActiveSession();

		Request request = Request.newMeRequest(session,
				new Request.GraphUserCallback() {
					@Override
					public void onCompleted(GraphUser user, Response response) {
						if (session == Session.getActiveSession()) {
							if (user != null) {
								String userId = null;
								userId = user.getId();
								String userName = user.getName();

								String userEmail = null;
								try {
									userEmail = user.asMap().get("email")
											.toString();
								} catch (Exception e) {
								}

								Intent data = null;

								if (userId != null) {
									data = new Intent();
									data.putExtra("userId", userId);
									data.putExtra("userName", userName);
								}

								if (getParent() == null) {
									setResult(Activity.RESULT_OK, data);
								} else {
									getParent().setResult(Activity.RESULT_OK,
											data);
								}
								finish();
							}
						}
						if (response.getError() != null) {
							// Handle errors, will do so later.
						}
					}
				});
		request.executeAsync();
	}

	private void onClickContinue() {
		ensureOpenSession();
	}

	@Override
	protected void onPause() {
		super.onPause();
		lifecycleHelper.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		lifecycleHelper.onResume();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		lifecycleHelper.onDestroy();
	}

	@Override
	protected void onStop() {
		super.onStop();
		lifecycleHelper.onStop();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		lifecycleHelper.onSaveInstanceState(outState);
	}
}
