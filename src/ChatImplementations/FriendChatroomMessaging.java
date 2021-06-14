package ChatImplementations;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import Interfaces.FriendChatroomInterface;
import SerializationClasses.Message;
import ServerPackage.Server;

public class FriendChatroomMessaging implements FriendChatroomInterface {

	@Override
	public synchronized boolean sendMessageToFriend(String clientName, String friendName, Message msg) {
		if (!checkIfFriends(clientName, friendName)) {
			System.out.println("These two people are not friends! " + clientName + ", " + friendName);
			return false;
		}
		
		String tableName = checkIfFriendChatroomExists(clientName, friendName);

		if (tableName.equals("")) {
			tableName = createFriendChatroom(clientName, friendName);
		}
		
		PreparedStatement statement = null;

		try {
			String sql = "INSERT INTO " + tableName + " (name, message, time) VALUES (?, ?, UTC_DATE())";
			statement = Server.db.dbConn.prepareStatement(sql);

			statement.setString(1, clientName);
			statement.setString(2, msg.getMsg());

			statement.executeUpdate();

			clientHasReadMessages(clientName, friendName, tableName);

			return true;
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			try {
				if (statement != null) {
					statement.close();
				}
			} catch (SQLException e) {
				System.out.println(e.getMessage());
			}
		}
		return false;
	}

	@Override
	public synchronized String getFriendChatroomDetails(String clientName, String friendName, int recentID) {
		if (!checkIfFriends(clientName, friendName))
			return "";

		String tableName = checkIfFriendChatroomExists(clientName, friendName);
		if (tableName.equals("")) {
			createFriendChatroom(clientName, friendName);
			return "";
		}
		
		PreparedStatement statement = null;
		ResultSet result = null;

		try {
			String sql = "SELECT * FROM " + tableName + " WHERE ID >= " + recentID + " ORDER BY ID ASC";;
			statement = Server.db.dbConn.prepareStatement(sql);

			result = statement.executeQuery();

			String jsonResult = "";
			JSONArray jsonArr = new JSONArray();
			while (result.next()) {
				String name = result.getString("name").toString();
				String msg = result.getString("message").toString();
				String time = result.getString("time").toString();

				Message message = new Message(name, msg, time);
				JSONObject obj = new JSONObject(message);
				jsonArr.put(obj);

				jsonResult = jsonArr.toString();
			}

			if (!jsonResult.equals(""))
				clientHasReadMessages(clientName, friendName, tableName);

			return jsonResult;

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			try {
				if (statement != null) {
					statement.close();
					result.close();
				}
			} catch (SQLException e) {
				System.out.println(e.getMessage());
			}
		}

		return "";
	}

	@Override
	/*
	 * Will check the friend's messages and notifies the client if there are new
	 * messages and they have not read them yet.
	 */
	public synchronized ArrayList<String> checkForNewFriendMessages(String name) {
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try {
			ArrayList<String> newNotificationFromFriends = new ArrayList<String>();

			String sql = "SELECT * FROM Friends_" + name + " ORDER BY ID ASC";
			statement = Server.db.dbConn.prepareStatement(sql);

			result = statement.executeQuery();

			// Check to see if there has been new messages since last time the user read it
			while (result.next()) {
				String friendName = result.getString("friendName");
				int lastReadMessagesTotal = result.getInt("last_row_count");

				String tableName = checkIfFriendChatroomExists(name, friendName);
				
				if (tableName.equals(""))
					continue;
				
				int curMsgTotal = getTotalMessages(name, friendName, tableName);

				if (curMsgTotal > lastReadMessagesTotal) {
					newNotificationFromFriends.add(friendName);
				}
			}

			if (newNotificationFromFriends.size() > 0) {
				return newNotificationFromFriends;
			}
			
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			try {
				if (statement != null) {
					statement.close();
					result.close();
				}
			} catch (SQLException e) {
				System.out.println(e.getMessage());
			}
		}
		return null;
	}

	@Override
	public synchronized boolean checkIfFriends(String clientName, String friendName) {
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try {
			String sql = "SELECT * FROM Friends_" + clientName + " WHERE friendName = ?";
			statement = Server.db.dbConn.prepareStatement(sql);

			statement.setString(1, friendName);
			result = statement.executeQuery();

			if (result.next()) {
				return true;
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			try {
				if (statement != null) {
					statement.close();
					result.close();
				}
			} catch (SQLException e) {
				System.out.println(e.getMessage());
			}
		}
		return false;
	}

	@Override
	public String createFriendChatroom(String clientName, String friendName) {
		PreparedStatement statement = null;
		
		try {
			String tableName = "FriendChatroom_" + clientName + "_" + friendName;
			String sql = "CREATE TABLE " + tableName
					+ " (ID INT NOT NULL AUTO_INCREMENT PRIMARY KEY, name char(16) NOT NULL, message varChar(255) NOT NULL, time DATE NOT NULL)";
			statement = Server.db.dbConn.prepareStatement(sql);

			statement.executeUpdate();
			
			return tableName;
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			try {
				if (statement != null) {
					statement.close();
				}
			} catch (SQLException e) {
				System.out.println(e.getMessage());
			}
		}
		return "";
	}

	@Override
	public String checkIfFriendChatroomExists(String clientName, String friendName) {
		PreparedStatement statement = null;
		PreparedStatement statement2 = null;
		ResultSet result = null;
		ResultSet result2 = null;
		
		try {
			String tableName = "FriendChatroom_" + clientName + "_" + friendName;
			String sql = "SHOW TABLES LIKE '" + tableName + "'";
			statement = Server.db.dbConn.prepareStatement(sql);

			result = statement.executeQuery();
			
			if (result.next()) {
				return tableName;
			}
			
			tableName = "FriendChatroom_" + friendName + "_" + clientName;
			sql = "SHOW TABLES LIKE '" + tableName + "'";
			statement2 = Server.db.dbConn.prepareStatement(sql);

			result2 = statement2.executeQuery();

			if (result2.next()) {
				return tableName;
			}

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			try {
				if (statement != null) {
					statement.close();
					result.close();
				}
				
				if (statement2 != null ) {
					statement2.close();
					result2.close();
				}
			} catch (SQLException e) {
				System.out.println(e.getMessage());
			}
		}
		return "";
	}

	@Override
	public int getTotalMessages(String name, String friendName, String tableName) {
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try {
			String sql = "SELECT COUNT(*) FROM " + tableName;
			statement = Server.db.dbConn.prepareStatement(sql);
			result = statement.executeQuery();

			int messagesTotal = 0;
			if (result.next()) {
				messagesTotal = result.getInt("COUNT(*)");

				return messagesTotal;
			}
			System.out.println("Error when calculating total # of rows for friend " + friendName + " from " + name);

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			try {
				if (statement != null) {
					statement.close();
					result.close();
				}
			} catch (SQLException e) {
				System.out.println(e.getMessage());
			}
		}
		return 0;
	}

	@Override
	public void clientHasReadMessages(String name, String id, String tableName) {
		PreparedStatement statement = null;
		
		try {

			int messagesTotal = 0;

			messagesTotal = getTotalMessages(name, id, tableName);

			String sql = "UPDATE Friends_" + name + " SET last_row_count = " + messagesTotal + " WHERE friendName = ?";

			statement = Server.db.dbConn.prepareStatement(sql);
			statement.setString(1, id);
			statement.executeUpdate();

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			try {
				if (statement != null) {
					statement.close();
				}
			} catch (SQLException e) {
				System.out.println(e.getMessage());
			}
		}
	}
}