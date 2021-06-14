package ChatImplementations;

import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import Interfaces.AuthenticationInterface;
import SerializationClasses.Friend;
import ServerPackage.ClientHandler;
import ServerPackage.Server;

public class AuthenticationManager implements AuthenticationInterface {

	private Random random = new SecureRandom();

	@Override
	public synchronized boolean registerAccount(String user, String password) {
		if (!checkIfAccountTableExists()) {
			createAccountTable();
		}

		PreparedStatement statement = null;

		try {
			String salt = generateSalt();

			String sql = "INSERT INTO Accounts(USERNAME, PSWD, SALT) VALUES(?, SHA2(?, 256), ?)";
			statement = Server.db.dbConn.prepareStatement(sql);

			statement.setString(1, user);
			statement.setString(2, password);
			statement.setString(3, salt);

			int result = statement.executeUpdate();

			if (result > 0) {
				if (createFriendsTable(user) & createChatroomTable(user))
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
	public synchronized boolean tryLogIn(String user, String password) {
		if (!checkIfAccountTableExists()) {
			createAccountTable();
			return false;
		}

		PreparedStatement statement = null;
		ResultSet result = null;

		try {
			String sql = "SELECT * FROM Accounts WHERE USERNAME = ?";
			statement = Server.db.dbConn.prepareStatement(sql);
			statement.setString(1, user);

			result = statement.executeQuery();

			if (result.next()) {
				String inputHash = getPasswordHash(password);

				String origHash = result.getString("PSWD");
				String salt = result.getString("SALT");

				if (isPasswordEqual(inputHash, origHash, salt)) {
					System.out.println(result.getString("USERNAME") + " Logged in!");
					
					return true;
				} else {
					System.out.println("Wrong password for: " + result.getString("USERNAME"));
				}
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
	public synchronized String retrieveSavedChatrooms(String name) {
		PreparedStatement statement = null;
		ResultSet result = null;
		;

		try {
			String sql = "SELECT * FROM SavedChatroom_" + name + " ORDER BY ID ASC";
			statement = Server.db.dbConn.prepareStatement(sql);

			result = statement.executeQuery();

			String jsonResult = "";
			JSONObject jsonObj = new JSONObject();

			int i = 0;

			while (result.next()) {
				String chatID = result.getString("chatID").toString();
				jsonObj.put(Integer.toString(i), chatID.toUpperCase());
				i++;
			}

			jsonResult = jsonObj.toString();
			return jsonResult;

		} catch (SQLException | JSONException e) {
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
	public synchronized boolean saveChatroomID(String name, String chatID) {
		PreparedStatement statement = null;

		try {
			String sql = "INSERT INTO SavedChatroom_" + name + " (chatID, last_row_count) VALUES (?, ?)";
			statement = Server.db.dbConn.prepareStatement(sql);

			statement.setString(1, chatID);
			statement.setInt(2, 0);

			int result = statement.executeUpdate();

			if (result > 0)
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
	public synchronized boolean removeChatroomID(String name, String chatID) {
		PreparedStatement statement = null;

		try {
			String sql = "DELETE FROM SavedChatroom_" + name + " WHERE chatID = ?";
			statement = Server.db.dbConn.prepareStatement(sql);

			statement.setString(1, chatID);

			int result = statement.executeUpdate();

			if (result > 0)
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
	public synchronized String retrieveFriends(String name) {
		PreparedStatement statement = null;
		ResultSet result = null;

		try {
			String sql = "SELECT * FROM Friends_" + name + " ORDER BY ID ASC";
			statement = Server.db.dbConn.prepareStatement(sql);

			result = statement.executeQuery();

			String jsonResult = "";
			JSONArray jsonObj = new JSONArray();

			while (result.next()) {
				String friendName = result.getString("friendName").toString();

				boolean active = false;
				active = isUserActive(friendName);

				Friend friend = new Friend(friendName, active);

				JSONObject obj = new JSONObject(friend);
				jsonObj.put(obj);
			}

			jsonResult = jsonObj.toString();
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
	public boolean createFriendsTable(String name) {
		PreparedStatement statement = null;

		try {
			String sql = "CREATE TABLE Friends_" + name
					+ " (ID INT NOT NULL AUTO_INCREMENT PRIMARY KEY, friendName varchar(255) UNIQUE NOT NULL, friendsSince DATE NOT NULL, last_row_count INT NOT NULL)";
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

		System.out.println("Failed to create friends table for " + name);
		return false;
	}

	@Override
	public boolean createChatroomTable(String name) {
		PreparedStatement statement = null;
		try {
			String sql = "CREATE TABLE SavedChatroom_" + name
					+ " (ID INT NOT NULL AUTO_INCREMENT PRIMARY KEY, chatID char(32) UNIQUE NOT NULL, last_row_count INT NOT NULL)";
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

		System.out.println("Failed to create chatroom table for " + name);
		return false;
	}

	@Override
	public boolean checkIfAccountExists(String name) {
		PreparedStatement statement = null;
		ResultSet result = null;
		try {
			String sql = "SELECT * FROM Accounts WHERE USERNAME = ?";
			statement = Server.db.dbConn.prepareStatement(sql);

			statement.setString(1, name);
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

	public boolean checkIfAccountTableExists() {
		PreparedStatement statement = null;
		ResultSet result = null;

		try {
			String sql = "SHOW TABLES LIKE 'Accounts'";
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

	public boolean createAccountTable() {
		PreparedStatement statement = null;
		try {
			String sql = "CREATE TABLE Accounts (ID INT NOT NULL PRIMARY KEY AUTO_INCREMENT, USERNAME varchar(255) UNIQUE NOT NULL, PSWD char(64) NOT NULL, SALT char(16))";
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

	@Override
	public boolean isUserActive(String name) {
		for (ClientHandler c : Server.clients) {
			if (c.isConnected() && c.getUserName().equals(name))
				return true;
		}

		return false;
	}

	private String getPasswordHash(String password) {
		String sql = "SELECT SHA2(?, 256)";
		PreparedStatement statement = null;
		ResultSet hashPaswdResult = null;

		try {
			statement = Server.db.dbConn.prepareStatement(sql);

			statement.setString(1, password);

			hashPaswdResult = statement.executeQuery();
			hashPaswdResult.next();

			String hash = hashPaswdResult.getString(1);

			return hash;
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			try {
				if (statement != null) {
					statement.close();
					hashPaswdResult.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return "";
	}

	/**
	 * Generates a 8 byte random salt string
	 * 
	 * @returns the salt as a string
	 */
	public String generateSalt() {
		byte[] salt = new byte[8];
		random.nextBytes(salt);

		return Base64.getEncoder().encodeToString(salt);
	}

	public boolean isPasswordEqual(String inputHash, String origHash, String salt) {
		return (origHash + salt).equals(inputHash + salt);
	}
}