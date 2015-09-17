package com.threebytes.callservicesamplefb;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.widget.Toast;

import com.facebook.FacebookException;
import com.facebook.Session;
import com.facebook.Session.NewPermissionsRequest;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.FriendPickerFragment;
import com.facebook.widget.PickerFragment;

import java.util.ArrayList;
import java.util.List;

public class PickFriendsActivity extends FragmentActivity {

	@SuppressWarnings("serial")
	private static final List<String> PERMISSIONS = new ArrayList<String>() {
		{
			add("user_friends");
			add("public_profile");
		}
	};

	private UiLifecycleHelper lifecycleHelper;
	private Bundle savedInstanceState;

	FriendPickerFragment friendPickerFragment;

	public static void populateParameters(Intent intent, String userId,
			boolean multiSelect, boolean showTitleBar) {
		intent.putExtra(FriendPickerFragment.USER_ID_BUNDLE_KEY, userId);
		intent.putExtra(FriendPickerFragment.MULTI_SELECT_BUNDLE_KEY,
				multiSelect);
		intent.putExtra(FriendPickerFragment.SHOW_TITLE_BAR_BUNDLE_KEY,
				showTitleBar);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.pick_friends_activity);

		lifecycleHelper = new UiLifecycleHelper(this,
				new Session.StatusCallback() {
					@Override
					public void call(Session session, SessionState state,
									 Exception exception) {
						onSessionStateChanged(session, state, exception);
					}
				});
		lifecycleHelper.onCreate(savedInstanceState);

		this.savedInstanceState = savedInstanceState;

		loadFriendPicketFragment();

		ensureOpenSession();

	}

	private void loadFriendPicketFragment() {
		FragmentManager fm = getSupportFragmentManager();

		if (savedInstanceState == null) {
			final Bundle args = getIntent().getExtras();
			friendPickerFragment = new FriendPickerFragment(args);
			friendPickerFragment.setTitleText("Select and Call");
			friendPickerFragment.setDoneButtonText("Call");

			fm.beginTransaction()
					.add(R.id.friend_picker_fragment, friendPickerFragment)
					.commit();
		} else {
			friendPickerFragment = (FriendPickerFragment) fm
					.findFragmentById(R.id.friend_picker_fragment);
		}

		friendPickerFragment
				.setOnErrorListener(new PickerFragment.OnErrorListener() {
					@Override
					public void onError(PickerFragment<?> fragment,
										FacebookException error) {
						PickFriendsActivity.this.onError(error);
					}
				});
		friendPickerFragment
		.setOnDoneButtonClickedListener(new PickerFragment.OnDoneButtonClickedListener() {
			@Override
			public void onDoneButtonClicked(PickerFragment<?> fragment) {
				FriendPickerData.setSelectedUsers(friendPickerFragment
						.getSelection());

				setResult(RESULT_OK, null);
				finish();
			}
		});

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
		if(friendPickerFragment == null) {
			String test;
					test= "0";
		}
		//friendPickerFragment.loadData(false);

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
									PickFriendsActivity.this,
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
			friendPickerFragment.loadData(false);
		}
	}

	private void onError(Exception error) {
		String text = getString(R.string.exception, error.getMessage());
		Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
		toast.show();
	}

	@Override
	protected void onStart() {
		super.onStart();

		try {
			List<GraphUser> selectedUsers = FriendPickerData.getSelectedUsers();
			if (selectedUsers != null) {
				FriendPickerData.setSelectedUsers(null);
				// friendPickerFragment.setSelection(selectedUsers);
			}

			// Load data, unless a query has already taken place.
			friendPickerFragment.loadData(false);
		} catch (Exception ex) {
			onError(ex);
		}
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
