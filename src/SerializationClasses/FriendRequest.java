package SerializationClasses;

public class FriendRequest {
	private String friendName;
	private String requestingName;
	
	public FriendRequest(String friendName, String requestingName) {
		this.setFriendName(friendName);
		this.setRequestingName(requestingName);
	}

	public String getFriendName() {
		return friendName;
	}

	public void setFriendName(String friendName) {
		this.friendName = friendName;
	}

	public String getRequestingName() {
		return requestingName;
	}

	public void setRequestingName(String requestingName) {
		this.requestingName = requestingName;
	}

}
