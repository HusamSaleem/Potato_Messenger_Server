package Interfaces;

public interface AuthenticationInterface {
	public boolean registerAccount(String user, String password);

	public boolean tryLogIn(String user, String password);

	public boolean createFriendsTable(String name);

	public boolean createChatroomTable(String name);

	public boolean checkIfAccountExists(String name);

	public String retrieveSavedChatrooms(String name);

	public boolean saveChatroomID(String name, String chatID);

	public boolean removeChatroomID(String name, String chatID);

	public String retrieveFriends(String name);

	public boolean isUserActive(String name);

}
