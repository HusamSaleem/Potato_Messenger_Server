package ChatImplementations;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import Interfaces.ChatroomInterface;
import SerializationClasses.Message;
import ServerPackage.Server;

public class PublicChatroom implements ChatroomInterface {

	@Override
	public synchronized boolean sendMessage(String chatID, Message msg) {
		PreparedStatement statement = null;

		try {
			if (!checkIfChatroomExists(chatID)) {
				if (!createNewChatroom(chatID)) {
					System.out.println("Failed to send the message");
					return false;
				}
			}
			String sql = "INSERT INTO Chatroom_" + chatID + " (name, message, time) VALUES (?, ?, UTC_DATE())";
			statement = Server.db.dbConn.prepareStatement(sql);

			statement.setString(1, msg.getName());
			statement.setString(2, msg.getMsg());

			int result = statement.executeUpdate();

			if (result > 0) {
				clientHasReadMessages(msg.getName(), chatID);
				return true;
			}
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
	/*
	 * Will check the saved chatrooms and notifies the client if there are new
	 * messages and they have not read them yet.
	 */
	public synchronized ArrayList<String> checkForNewMessages(String name) {
		PreparedStatement statement = null;
		ResultSet result = null;

		try {
			ArrayList<String> newNotificationFromChatroom = new ArrayList<String>();

			String sql = "SELECT * FROM SavedChatroom_" + name + " ORDER BY ID ASC";
			statement = Server.db.dbConn.prepareStatement(sql);

			result = statement.executeQuery();

			// Check to see if there has been new messages since last time the user read it
			while (result.next()) {
				String chatID = result.getString("chatID");
				int lastReadMessagesTotal = result.getInt("last_row_count");

				int curMsgTotal = getTotalMessages(chatID);

				if (curMsgTotal > lastReadMessagesTotal) {
					newNotificationFromChatroom.add(chatID);
				}
			}
			
			if (newNotificationFromChatroom.size() > 0)
				return newNotificationFromChatroom;

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
	public synchronized String getChatroomDetails(String clientName, String chatID, int recentID) {
		PreparedStatement statement = null;
		ResultSet result = null;

		try {
			if (!checkIfChatroomExists(chatID)) {
				if (createNewChatroom(chatID)) {
					return "";
				} else {
					return "";
				}
			}

			String sql = "SELECT * FROM Chatroom_" + chatID + " WHERE ID >= " + recentID + " ORDER BY ID ASC";

			statement = Server.db.dbConn.prepareStatement(sql);
			result = statement.executeQuery();

			String jsonResult = "";
			JSONArray jsonArr = new JSONArray();

			while (result.next()) {
				String name = result.getString("name");
				String msg = result.getString("message");
				String time = result.getString("time");

				Message message = new Message(name, msg, time);
				JSONObject obj = new JSONObject(message);
				jsonArr.put(obj);

				jsonResult = jsonArr.toString();
			}

			if (!jsonResult.equals(""))
				clientHasReadMessages(clientName, chatID);

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
	public int getTotalMessages(String chatID) {
		PreparedStatement statement = null;
		ResultSet result = null;

		try {
			String sql = "SELECT COUNT(*) FROM Chatroom_" + chatID.toUpperCase();
			statement = Server.db.dbConn.prepareStatement(sql);
			result = statement.executeQuery();

			int messagesTotal = 0;
			if (result.next()) {
				messagesTotal = result.getInt("COUNT(*)");

				return messagesTotal;
			} else {
				System.out.println("Error when calculating total # of rows for chatroom " + chatID);
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
		return 0;
	}

	@Override
	public void clientHasReadMessages(String name, String chatID) {
		PreparedStatement statement = null;

		try {

			int messagesTotal = 0;

			messagesTotal = getTotalMessages(chatID);

			String sql = "UPDATE SavedChatroom_" + name + " SET last_row_count = " + messagesTotal
					+ " WHERE chatID = ?";

			statement = Server.db.dbConn.prepareStatement(sql);
			statement.setString(1, chatID);
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

	@Override
	public boolean createNewChatroom(String chatID) {
		PreparedStatement statement = null;

		try {
			String sql = "CREATE TABLE Chatroom_" + chatID.toUpperCase()
					+ " (ID INT NOT NULL AUTO_INCREMENT PRIMARY KEY, name char(16) NOT NULL, message varChar(255) NOT NULL, time DATE NOT NULL)";
			statement = Server.db.dbConn.prepareStatement(sql);

			statement.executeUpdate();

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

		System.out.println("Failed to create Chatroom_" + chatID);
		return false;
	}

	@Override
	public boolean checkIfChatroomExists(String chatID) {
		PreparedStatement statement = null;
		ResultSet result = null;

		try {
			String sql = "SHOW TABLES LIKE 'Chatroom_" + chatID.toUpperCase() + "'";
			statement = Server.db.dbConn.prepareStatement(sql);

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
}