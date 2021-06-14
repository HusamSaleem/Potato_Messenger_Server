package Interfaces;

import java.util.ArrayList;

import SerializationClasses.Message;

public interface ChatroomInterface {
	public boolean sendMessage(String chatID, Message msg);

	public String getChatroomDetails(String clientName, String chatID, int recentID);

	public int getTotalMessages(String chatID);

	public void clientHasReadMessages(String name, String id);

	public ArrayList<String> checkForNewMessages(String name);

	public boolean checkIfChatroomExists(String chatID);

	public boolean createNewChatroom(String chatID);

}
