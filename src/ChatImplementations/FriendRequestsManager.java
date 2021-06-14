package ChatImplementations;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONArray;
import org.json.JSONObject;

import Interfaces.FriendRequestsInterface;
import SerializationClasses.FriendRequest;
import ServerPackage.Server;

public class FriendRequestsManager implements FriendRequestsInterface {

	@Override
	public synchronized boolean sendFriendRequest(String name, String friendName) {
		if (name.equals(friendName))
			return false;
		
		if (!doesFriendRequestTableExist())
			createGlobalFriendRequestTable();

		if (!Server.db.authManager.checkIfAccountExists(friendName))
			return false;

		if (checkIfFriends(name, friendName))
			return false;

		if (existingFriendRequest(name, friendName)) {
			return true;
		}

		PreparedStatement statement = null;
		try {
			String sql = "INSERT INTO Friend_Requests (requestingName, friendName) VALUES (?,?)";
			statement = Server.db.dbConn.prepareStatement(sql);

			statement.setString(1, name);
			statement.setString(2, friendName);

			int result = statement.executeUpdate();

			if (result > 0) {
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
	public synchronized String retrieveFriendRequests(String name) {
		if (!doesFriendRequestTableExist())
			createGlobalFriendRequestTable();

		PreparedStatement statement = null;
		ResultSet result = null;
		
		try {
			String sql = "SELECT * FROM Friend_Requests WHERE friendName = ? ORDER BY ID ASC";
			statement = Server.db.dbConn.prepareStatement(sql);
			statement.setString(1, name);

			result = statement.executeQuery();

			String jsonResult = "";
			JSONArray jsonArr = new JSONArray();

			while (result.next()) {
				String friendName = result.getString("friendName");
				String requestingName = result.getString("requestingName");

				FriendRequest friendReq = new FriendRequest(friendName, requestingName);
				JSONObject obj = new JSONObject(friendReq);
				jsonArr.put(obj);

				jsonResult = jsonArr.toString();
			}

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
	public synchronized boolean declineFriendRequest(String name, String requestingName) {
		if (checkIfFriends(name, requestingName))
			return false;

		if (deleteFriendRequest(requestingName, name))
			return true;

		return false;
	}

	@Override
	public synchronized boolean acceptFriendRequest(String name, String requestingName) {
		if (checkIfFriends(name, requestingName))
			return false;

		if (deleteFriendRequest(requestingName, name) && addFriend(name, requestingName))
			return true;

		return false;
	}
	
	@Override
	public synchronized boolean removeFriend(String name, String friendName) {
		PreparedStatement statement = null;
		
		try {
			String sql = "DELETE FROM Friends_" + name + " WHERE friendName = ?";
			statement = Server.db.dbConn.prepareStatement(sql);
			statement.setString(1, friendName);

			statement.executeUpdate();

			statement.close();
			
			sql = "DELETE FROM Friends_" + friendName + " WHERE friendName = ?";
			statement = Server.db.dbConn.prepareStatement(sql);

			statement.setString(1, name);
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
		return false;
	}
	
	@Override
	public boolean deleteFriendRequest(String name, String friendName) {
		PreparedStatement statement = null;
		PreparedStatement statement2 = null;
		
		try {
			String sql = "DELETE FROM Friend_Requests WHERE requestingName = ? and friendName = ?";
			String sql2 = "DELETE FROM Friend_Requests WHERE requestingName = ? and friendName = ?";
			statement = Server.db.dbConn.prepareStatement(sql);
			statement2 = Server.db.dbConn.prepareStatement(sql2);

			statement.setString(1, name);
			statement.setString(2, friendName);
			statement2.setString(1, friendName);
			statement2.setString(2, name);

			int result = statement.executeUpdate();
			int result2 = statement2.executeUpdate();

			if (result > 0 || result2 > 0) {
				return true;
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			try {
				if (statement != null) {
					statement.close();
				}
				if (statement2 != null) {
					statement2.close();
				}
			} catch (SQLException e) {
				System.out.println(e.getMessage());
			}
		}
		return false;
	}

	@Override
	public boolean addFriend(String name, String friendName) {
		PreparedStatement statement = null;
		PreparedStatement statement2 = null;
		
		try {
			String sql1 = "INSERT INTO Friends_" + name + " (friendName, friendsSince, last_row_count) VALUES (?, UTC_DATE(), 0)";
			String sql2 = "INSERT INTO Friends_" + friendName + " (friendName, friendsSince, last_row_count) VALUES (?, UTC_DATE(), 0)";
			statement = Server.db.dbConn.prepareStatement(sql1);
			statement2 = Server.db.dbConn.prepareStatement(sql2);

			statement.setString(1, friendName);
			statement2.setString(1, name);

			int result1 = statement.executeUpdate();
			int result2 = statement2.executeUpdate();

			if (result1 > 0 || result2 > 0) {
				return true;
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			try {
				if (statement != null) {
					statement.close();
				}
				if (statement2 != null) {
					statement2.close();
				}
			} catch (SQLException e) {
				System.out.println(e.getMessage());
			}
		}
		return false;
	}

	@Override
	public boolean existingFriendRequest(String name, String friendName) {
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try {
			String sql = "SELECT * FROM Friend_Requests WHERE requestingName = ? AND friendName = ? OR requestingName = ? AND friendName = ?";
			statement = Server.db.dbConn.prepareStatement(sql);
			statement.setString(1, name);
			statement.setString(2, friendName);
			statement.setString(3, friendName);
			statement.setString(4, name);

			result = statement.executeQuery();

			int count = 0;
			boolean existing = false;

			while (result.next()) {
				existing = true;
				count++;
			}

			// At least 1 friend request both ways
			if (count > 0) {
				addFriend(name, friendName);
				deleteFriendRequest(name, friendName);
			}
			
			return existing;

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
	public boolean checkIfFriends(String clientName, String friendName) {
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
	public boolean doesFriendRequestTableExist() {
		PreparedStatement statement = null;
		ResultSet result = null;
		
		try {
			String sql = "SHOW TABLES LIKE 'Friend_Requests'";
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

	@Override
	public boolean createGlobalFriendRequestTable() {
		PreparedStatement statement = null;
		
		try {
			String sql = "CREATE TABLE Friend_Requests (ID INT NOT NULL PRIMARY KEY AUTO_INCREMENT, requestingName char(64) NOT NULL, friendName char(64) NOT NULL)";
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

		return false;
	}
}