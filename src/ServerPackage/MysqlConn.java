package ServerPackage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import ChatImplementations.AuthenticationManager;
import ChatImplementations.FriendChatroomMessaging;
import ChatImplementations.FriendRequestsManager;
import ChatImplementations.PublicChatroom;

/**
 * <p>
 * <b> This is the database class where it handles the connectivity, as well as
 * the saving/updating/getting information </b>
 * </p>
 * 
 * @author Husam Saleem
 */
public class MysqlConn {
	public Connection dbConn = null;

	// CHANGE ME
	private String dbName = "Your mysql database name here";
	private String url = "jdbc:mysql://localhost:3306/" + dbName
			+ "?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
	private String username = "Your Username Here"; // MySQL username
	private String password = "Your Password Here"; // MySQL password // HIDE THIS!!!!

	public AuthenticationManager authManager = null;
	public FriendChatroomMessaging friendMessaging = null;
	public PublicChatroom publicChatroom = null;
	public FriendRequestsManager friendRequestsManager = null;

	public MysqlConn() {
		startConnection();

		authManager = new AuthenticationManager();
		friendMessaging = new FriendChatroomMessaging();
		publicChatroom = new PublicChatroom();
		friendRequestsManager = new FriendRequestsManager();
	}

	/**
	 * <p>
	 * <b> Starts the connection from the server to the database </b>
	 * </p>
	 */
	public void startConnection() {
		try {

			Class.forName("com.mysql.cj.jdbc.Driver");
			dbConn = DriverManager.getConnection(url, username, password);

			if (dbConn != null)
				System.out.println("Database Connection has been established!");
		} catch (SQLException | ClassNotFoundException e) {
			System.out.println(e.getMessage());
		}
	}
}