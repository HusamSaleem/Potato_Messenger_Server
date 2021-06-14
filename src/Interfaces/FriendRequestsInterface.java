package Interfaces;

public interface FriendRequestsInterface {
	public boolean deleteFriendRequest(String name, String friendName);

	public boolean addFriend(String name, String friendName);

	public boolean declineFriendRequest(String name, String requestingName);

	public boolean acceptFriendRequest(String name, String requestingName);

	public boolean existingFriendRequest(String name, String friendName);

	public boolean sendFriendRequest(String name, String friendName);

	public String retrieveFriendRequests(String name);

	public boolean checkIfFriends(String clientName, String friendName);

	public boolean doesFriendRequestTableExist();

	public boolean createGlobalFriendRequestTable();

	public boolean removeFriend(String name, String friendName);

}
