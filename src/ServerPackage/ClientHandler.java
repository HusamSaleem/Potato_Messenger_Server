package ServerPackage;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import SerializationClasses.Message;

/**
 * <p>
 * <b> Handles information sending and receiving by the client, and processes it
 * </b>
 * </p>
 * 
 * @author Husam Saleem
 */
public class ClientHandler implements Runnable {
	public final Socket S;
	public final String PROC_ID;

	private String userName;

	// These variables are for checking to see if the client is still connected.
	private int retryConnections;
	private boolean isConnected;
	private long lastPinged;

	private int mostRecentMsgID = 0;
	private int mostRecentFriendMsgID = 0;

	public ClientHandler(Socket clientSocket, String PROC_ID) {
		this.S = clientSocket;
		this.PROC_ID = PROC_ID;
		this.isConnected = true;

		this.userName = "";

		this.retryConnections = 0;
		this.lastPinged = System.currentTimeMillis();

		sendData("Process_ID: " + this.PROC_ID);
	}

	@Override
	public void run() {
		try {
			if (this.isConnected)
				processData();
		} catch (IOException e) {
			System.out.println(e.getMessage());
			this.isConnected = false;
		}
	}

	/**
	 * <p>
	 * <b> Sends data to the client if the client is connected </b>
	 * </p>
	 * 
	 * @param data, the string of data to be sent
	 * @return
	 * @throws IOException
	 */
	synchronized public boolean sendData(String data) {
		if (!this.isConnected)
			return false;

		// "`" means its the end of the data line
		try {
			DataOutputStream writer = new DataOutputStream(S.getOutputStream());
			writer.write((data + "`").getBytes());
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * <p>
	 * <b> Checks for incoming data through the socket connection from the client
	 * </b>
	 * </p>
	 * 
	 * @return a String[] array that contains all the data that the client sent
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public String[] recieveData() throws IOException, InterruptedException, SocketException {
		int red = -1;
		byte[] buffer = new byte[5 * 1024]; // A read buffer of 5 KiB
		byte[] redData;

		String clientData = "";
		String redDataText;

		// While there is still data available
		while ((red = S.getInputStream().read(buffer)) > -1) {
			redData = new byte[red];
			System.arraycopy(buffer, 0, redData, 0, red);
			redDataText = new String(redData, "UTF-8"); // The client sends UTF-8 Encoded
			clientData += redDataText;

			if (clientData.indexOf("`") != -1) {
				break;
			}
		}

		// Turn all the sent commands into an array in case if they get combined
		String[] data = clientData.split("`");
		return data;
	}

	/**
	 * <p>
	 * <b> Parses through the incoming data from the client and process it</b>
	 * </p>
	 * 
	 * @throws IOException
	 */
	public void processData() throws IOException {
		String[] data = null;
		try {
			data = recieveData();

			for (String s : data) {
				// Client pings back
				if (s.equals("Ping")) {
					lastPinged = System.currentTimeMillis();
				} else if (s.contains("Login: ")) {
					System.out.println(this.S + " Sent: " + s);
					logIn(s);
				} else if (s.contains("Register: ")) {
					System.out.println(this.S + " Sent: " + s);
					register(s);
				} else if (s.contains("Send Message: ")) {
					sendMessage(s);
				} else if (s.contains("Retrieve Messages: ")) {
					System.out.println(s);
					retrieveChatIDMessages(s);
				} else if (s.contains("Send Friend Message: ")) {
					System.out.println(s);
					sendMessageToFriend(s);
				} else if (s.contains("Retrieve Friend Messages: ")) {
					System.out.println(s);
					retrieveFriendMessages(s);
				} else if (s.contains("Request to add Friend: ")) {
					System.out.println(s);
					requestFriendship(s);
				} else if (s.contains("Retrieve ChatIDs: ")) {
					System.out.println(s);
					retrieveSavedChatIDS(s);
				} else if (s.contains("Add Chat ID: ")) {
					System.out.println(s);
					addChatID(s);
				} else if (s.contains("Remove Chat ID: ")) {
					System.out.println(s);
					removeChatID(s);
				} else if (s.contains("Retrieve Friends: ")) {
					System.out.println(s);
					retrieveFriends(s);
				} else if (s.contains("Remove Friend: ")) {
					System.out.println(s);
					removeFriend(s);
				} else if (s.contains("Accept Friend Request: ")) {
					System.out.println(s);
					acceptFriendRequest(s);
				} else if (s.contains("Decline Friend Request: ")) {
					System.out.println(s);
					declineFriendRequest(s);
				} else if (s.contains("Retrieve Friend Requests: ")) {
					System.out.println(s);
					retrieveFriendRequests(s);
				} else if (s.contains("Check Chatroom Notifications: ")) {
					System.out.println(s);
					checkIfNewNotificationsExist(s);
				} else if (s.contains("Check Friend Notifications: ")) {
					System.out.println(s);
					checkIfNewFriendNotificationsExist(s);
				} else if (s.equals("Delete recent cache")) {
					mostRecentMsgID = 0;
				} else if (s.equals("Delete recent friend cache")) {
					mostRecentFriendMsgID = 0;
				}
			}

		} catch (IOException | InterruptedException e) {
			System.out.println(e.getMessage());
			System.out.println("Something went wrong when processing data... with client: " + S.toString());
			this.isConnected = false;
			return;
		}
	}

	/** START OF AUTH MANAGER METHODS **/

	/**
	 * Tries to log in to the specified account
	 * 
	 * @param jsonString
	 */
	private void logIn(String jsonString) {
		String jsonStr = jsonString.substring(jsonString.indexOf(":") + 1);
		JSONObject json;
		try {
			json = new JSONObject(jsonStr);
			String username = json.getString("username").toLowerCase();
			String password = json.getString("password");

			boolean success = Server.db.authManager.tryLogIn(username, password);

			if (!success) {
				sendData("Login Failure");
			} else {
				setUserName(username.toLowerCase());
				sendData(username + ":Login Success");
			}

		} catch (JSONException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Tries to register the account
	 * 
	 * @param jsonString
	 */
	private void register(String jsonString) {
		String jsonStr = jsonString.substring(jsonString.indexOf(":") + 1);
		JSONObject json;
		try {
			json = new JSONObject(jsonStr);
			String username = json.getString("username").toLowerCase();
			String password = json.getString("password");

			boolean success = Server.db.authManager.registerAccount(username, password);

			if (!success) {
				sendData("Register Failure");
			} else {
				setUserName(username.toLowerCase());
				sendData(username + ":Register Success");
			}

		} catch (JSONException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Retrieves all the saved chatroom ids for the user
	 * 
	 * @param jsonString
	 */
	private void retrieveSavedChatIDS(String jsonString) {
		String json = jsonString.substring(jsonString.indexOf(":") + 1).trim();

		try {
			JSONObject jsonObj = new JSONObject(json);

			String jsonResult = Server.db.authManager.retrieveSavedChatrooms(jsonObj.getString("name").toLowerCase());

			sendData("JSON SAVED CHATROOMS: " + jsonResult);
		} catch (JSONException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Adds a chat room id to a user requesting it
	 * 
	 * @param jsonString
	 */
	private void addChatID(String jsonString) {
		String json = jsonString.substring(jsonString.indexOf(":") + 1).trim();

		boolean success = false;
		try {
			JSONObject jsonObj = new JSONObject(json);
			String name = jsonObj.getString("name").toLowerCase();

			success = Server.db.authManager.saveChatroomID(name, jsonObj.getString("chatID").toUpperCase());

			String jsonResult = Server.db.authManager.retrieveSavedChatrooms(name);
			sendData("JSON SAVED CHATROOMS: " + jsonResult);
		} catch (JSONException e) {
			System.out.println(e.getMessage());
		}

		if (success)
			System.out.println("Successfully added chatroom ID for client: " + this.S.toString());
	}

	/**
	 * Removes a saved chatroom id
	 * 
	 * @param jsonString
	 */
	private void removeChatID(String jsonString) {
		String json = jsonString.substring(jsonString.indexOf(":") + 1).trim();

		boolean success = false;
		try {
			JSONObject jsonObj = new JSONObject(json);
			String name = jsonObj.getString("name").toLowerCase();

			success = Server.db.authManager.removeChatroomID(name, jsonObj.getString("chatID").toUpperCase());

			String jsonResult = Server.db.authManager.retrieveSavedChatrooms(name);
			sendData("JSON SAVED CHATROOMS: " + jsonResult);
		} catch (JSONException e) {
			System.out.println(e.getMessage());
		}

		if (success)
			System.out.println("Successfully removed chatroom ID for client: " + this.S.toString());
	}

	/**
	 * Retrieves the friend list of said specified user
	 * 
	 * @param jsonString
	 */
	private void retrieveFriends(String jsonString) {
		String json = jsonString.substring(jsonString.indexOf(":") + 1).trim();

		try {
			JSONObject jsonObj = new JSONObject(json);
			String jsonResult = Server.db.authManager.retrieveFriends(jsonObj.getString("name").toLowerCase());

			sendData("JSON FRIEND LIST: " + jsonResult);
		} catch (JSONException e) {
			System.out.println(e.getMessage());
		}
	}

	/** END OF AUTH MANAGER METHODS **/

	/** START OF PUBLIC CHATROOM METHODS **/

	/**
	 * Sending a message to a specified public chat room
	 * 
	 * @param jsonString
	 */
	private void sendMessage(String jsonString) {
		String json = jsonString.substring(jsonString.indexOf(":") + 1).trim();

		boolean success = false;
		try {
			JSONObject jsonObj = new JSONObject(json);

			String chatID = jsonObj.getString("chatID").toString().toUpperCase();
			Message msg = new Message(jsonObj.getString("user").toString().toLowerCase(),
					jsonObj.getString("msg").toString(), null);

			success = Server.db.publicChatroom.sendMessage(chatID, msg);

			if (success) {
				System.out.println(msg.getName() + " Sent this message: " + msg.getMsg() + " To chat ID: " + chatID);
				sendData("Message successfully sent!");

				String jsonResult = Server.db.publicChatroom.getChatroomDetails(getUserName(), chatID,
						this.mostRecentMsgID + 1);

				if (!jsonResult.equals(""))
					this.mostRecentMsgID += new JSONArray(jsonResult).length();
				sendData("JSON CHATROOM: " + jsonResult);
			}
		} catch (JSONException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Checks to see if any new messages were sent to the user's saved chatrooms
	 * 
	 * @param jsonString
	 */
	private void checkIfNewNotificationsExist(String jsonString) {
		String jsonStr = jsonString.substring(jsonString.indexOf(":") + 1);

		try {
			JSONObject json = new JSONObject(jsonStr);
			String name = json.getString("name").toLowerCase();

			ArrayList<String> list = Server.db.publicChatroom.checkForNewMessages(name);

			if (list == null) {
				sendData("No new notifications");
			} else {
				JSONObject returnJson = new JSONObject();

				for (int i = 0; i < list.size(); i++) {
					returnJson.put(Integer.toString(i), list.get(i));
				}

				String jsonResult = returnJson.toString();

				sendData("NEW NOTIFICATIONS: " + jsonResult);
			}
		} catch (JSONException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Retrieves the latest messages from specified chat room id and sends it back
	 * to the user
	 * 
	 * @param str
	 */
	private void retrieveChatIDMessages(String str) {
		String chatID = str.substring(str.indexOf(":") + 1).trim().toUpperCase();
		String jsonResult = Server.db.publicChatroom.getChatroomDetails(getUserName(), chatID,
				this.mostRecentMsgID + 1);

		try {
			if (!jsonResult.equals(""))
				this.mostRecentMsgID += new JSONArray(jsonResult).length();
			sendData("JSON CHATROOM: " + jsonResult);
		} catch (JSONException e) {
			System.out.println(e.getMessage());
		}
	}

	/** END OF PUBLIC CHATROOM METHODS **/

	/** START OF FRIEND CHATROOM MESSAGES METHODS **/

	/**
	 * Checks to see if any of the user's friends has sent new messages to them
	 * 
	 * @param jsonString
	 */
	private void checkIfNewFriendNotificationsExist(String jsonString) {
		String jsonStr = jsonString.substring(jsonString.indexOf(":") + 1);

		try {
			JSONObject json = new JSONObject(jsonStr);
			String name = json.getString("name").toLowerCase();

			ArrayList<String> list = Server.db.friendMessaging.checkForNewFriendMessages(name);

			if (list == null) {
				sendData("No new notifications");
			} else {
				JSONObject returnJson = new JSONObject();

				for (int i = 0; i < list.size(); i++) {
					returnJson.put(Integer.toString(i), list.get(i));
				}

				String jsonResult = returnJson.toString();

				sendData("NEW FRIEND NOTIFICATIONS: " + jsonResult);
			}
		} catch (JSONException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Sends a message from user to friend
	 * 
	 * @param jsonString
	 */
	private void sendMessageToFriend(String jsonString) {
		String json = jsonString.substring(jsonString.indexOf(":") + 1).trim();

		boolean success = false;
		try {
			JSONObject jsonObj = new JSONObject(json);

			String name = jsonObj.getString("name").toLowerCase();
			String friendName = jsonObj.getString("friendName").toLowerCase();

			Message msg = new Message(name, jsonObj.getString("msg"), null);
			success = Server.db.friendMessaging.sendMessageToFriend(name, friendName, msg);

			if (success) {
				System.out.println(msg.getName() + " Sent this message: " + msg.getMsg() + " To friend: " + friendName);
				sendData("Message successfully sent!");

				String jsonResult = Server.db.friendMessaging.getFriendChatroomDetails(name, friendName,
						mostRecentFriendMsgID + 1);
				if (!jsonResult.equals(""))
					this.mostRecentFriendMsgID += new JSONArray(jsonResult).length();
				sendData("JSON FRIEND CHATROOM: " + jsonResult);
			}

		} catch (JSONException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Retrieves the users's friend's messages on request
	 * 
	 * @param jsonString
	 */
	private void retrieveFriendMessages(String jsonString) {
		String json = jsonString.substring(jsonString.indexOf(":") + 1).trim();

		try {
			JSONObject jsonObj = new JSONObject(json);

			String jsonResult = Server.db.friendMessaging.getFriendChatroomDetails(
					jsonObj.getString("name").toLowerCase(), jsonObj.getString("friendName").toLowerCase(),
					mostRecentFriendMsgID + 1);

			if (!jsonResult.equals(""))
				mostRecentFriendMsgID += new JSONArray(jsonResult).length();
			sendData("JSON FRIEND CHATROOM: " + jsonResult);
		} catch (JSONException e) {
			System.out.println(e.getMessage());
		}
	}

	/** END OF FRIEND CHATROOM MESSAGES METHODS **/

	/** START OF FRIEND REQUESTS METHODS **/

	/**
	 * Sends a friend request from user to friend
	 * 
	 * @param jsonString
	 */
	private void requestFriendship(String jsonString) {
		String json = jsonString.substring(jsonString.indexOf(":") + 1).trim();

		boolean success = false;
		try {
			JSONObject jsonObj = new JSONObject(json);
			String name = jsonObj.getString("name").toLowerCase();
			String friendName = jsonObj.getString("friendName").toLowerCase();

			success = Server.db.friendRequestsManager.sendFriendRequest(name, friendName);
		} catch (JSONException e) {
			System.out.println(e.getMessage());
		}

		if (success)
			System.out.println("Client: " + this.S.toString() + " has requested someone to be their friend");
	}

	/**
	 * Retrieves the friend requests of said user on request
	 * 
	 * @param jsonString
	 */
	private void retrieveFriendRequests(String jsonString) {
		String jsonStr = jsonString.substring(jsonString.indexOf(":") + 1);

		try {
			JSONObject json = new JSONObject(jsonStr);
			String name = json.getString("name").toLowerCase();

			String jsonResult = Server.db.friendRequestsManager.retrieveFriendRequests(name);

			sendData("JSON FRIEND REQUESTS: " + jsonResult);
		} catch (JSONException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Accepts a friend request
	 * 
	 * @param jsonString
	 */
	private void acceptFriendRequest(String jsonString) {
		String jsonStr = jsonString.substring(jsonString.indexOf(":") + 1);

		try {
			JSONObject json = new JSONObject(jsonStr);
			String name = json.getString("friendName").toLowerCase();
			String requestingName = json.getString("requestingName").toLowerCase();

			boolean success = Server.db.friendRequestsManager.acceptFriendRequest(name, requestingName);

			if (!success) {
				sendData("Failed to accept friend request");
			} else {
				String jsonResult = Server.db.friendRequestsManager.retrieveFriendRequests(name);
				sendData("Friend request successfully accepted");
				sendData("JSON FRIEND REQUESTS: " + jsonResult);
			}
		} catch (JSONException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Declines a friend request
	 * 
	 * @param jsonString
	 */
	private void declineFriendRequest(String jsonString) {
		String jsonStr = jsonString.substring(jsonString.indexOf(":") + 1);

		try {
			JSONObject json = new JSONObject(jsonStr);
			String name = json.getString("friendName").toLowerCase();
			String requestingName = json.getString("requestingName").toLowerCase();

			boolean success = Server.db.friendRequestsManager.declineFriendRequest(name, requestingName);

			if (!success) {
				sendData("Failed to decline friend request");
			} else {
				String jsonResult = Server.db.friendRequestsManager.retrieveFriendRequests(name);
				sendData("Friend request successfully declined");
				sendData("JSON FRIEND REQUESTS: " + jsonResult);
			}
		} catch (JSONException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Removes a friend from the user's friend list
	 * 
	 * @param jsonString
	 */
	private void removeFriend(String jsonString) {
		String json = jsonString.substring(jsonString.indexOf(":") + 1).trim();

		boolean success = false;
		try {
			JSONObject jsonObj = new JSONObject(json);
			success = Server.db.friendRequestsManager.removeFriend(jsonObj.getString("name").toLowerCase(),
					jsonObj.getString("friendName").toLowerCase());

			String jsonResult = Server.db.authManager.retrieveFriends(jsonObj.getString("name").toLowerCase());
			sendData("JSON FRIEND LIST: " + jsonResult);
		} catch (JSONException e) {
			System.out.println(e.getMessage());
		}

		if (!success)
			System.out.println("Failed to remove saved friend for client: " + this.S.toString());
	}

	/** END OF FRIEND REQUEST METHODS **/

	/**
	 * @return false if the amount of connection retries is greater than 3
	 */
	public boolean keepAlive() {
		if (this.retryConnections > 3) {
			return false;
		} else {
			return true;
		}
	}

	public int getRetryCount() {
		return this.retryConnections;
	}

	public void increaseRetryCount() {
		this.retryConnections++;
	}

	public long getLastPingTime() {
		return this.lastPinged;
	}

	public boolean isConnected() {
		return this.isConnected;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public int getMostRecentMsgID() {
		return mostRecentMsgID;
	}

	public void setMostRecentMsgID(int mostRecentMsgID) {
		this.mostRecentMsgID = mostRecentMsgID;
	}

	public int getMostRecentFriendMsgID() {
		return mostRecentFriendMsgID;
	}

	public void setMostRecentFriendMsgID(int mostRecentFriendMsgID) {
		this.mostRecentFriendMsgID = mostRecentFriendMsgID;
	}
}