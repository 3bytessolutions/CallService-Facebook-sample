package com.threebytes.callservicesamplefb;

import android.app.Application;

import com.facebook.model.GraphUser;

import java.util.List;

// We use a custom Application class to store our minimal state data (which users have been selected).
// A real-world application will likely require a more robust data model.
public class FriendPickerData {
	private static List<GraphUser> selectedUsers;

	public static List<GraphUser> getSelectedUsers() {
		return selectedUsers;
	}

	public static void setSelectedUsers(List<GraphUser> newSelectedUsers) {
		selectedUsers = newSelectedUsers;
	}
}
