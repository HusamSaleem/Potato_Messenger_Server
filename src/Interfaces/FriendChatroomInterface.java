package Interfaces;

import java.util.ArrayList;

import SerializationClasses.Message;

public interface FriendChatroomInterface {
	public boolean sendMessageToFriend(String clientName, String friendName, Message msg);

	public String getFriendChatroomDetails(String clientName, String friendName, int recentID);

	public boolean checkIfFriends(String clientName, String friendName);

	public String checkIfFriendChatroomExists(String clientName, String friendName);

	public int getTotalMessages(String name, String friendName, String tableName);

	public void clientHasReadMessages(String name, String friendName, String tableName);

	public ArrayList<String> checkForNewFriendMessages(String name);

	public String createFriendChatroom(String clientName, String friendName);
}
