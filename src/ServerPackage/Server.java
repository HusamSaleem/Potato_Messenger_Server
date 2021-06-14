package ServerPackage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;

import javax.net.ServerSocketFactory;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Husam Saleem
 */
public class Server {

	public static ArrayList<ClientHandler> clients = new ArrayList<ClientHandler>();
	ServerSocketFactory serverSocketFactory;
	ServerSocket serverSocket;
	private static int PORT = 9663; // CHANGE ME IF YOU WANT

	private int proc_ID_Counter = 1;

	public static ExecutorService threadPool;
	private static final int POOL_SIZE = 25;

	public static MysqlConn db;

	// Some constants
	// The interval for when pings go out to clients,
	private final long PING_INTERVAL = 60000; // 60 seconds
	private final int DATA_LISTEN_INTERVAL = 50; // 50 Milliseconds
	private final long KEEP_DATABASE_ALIVE_INTERVAL = 60000; // 60 seconds
	private final long INIT_DELAY = 10000; // 10 seconds

	public Server(int port, int poolSize) throws IOException {
		this.serverSocketFactory = (ServerSocketFactory) ServerSocketFactory.getDefault();
		serverSocket = (ServerSocket) serverSocketFactory.createServerSocket(port);
		threadPool = Executors.newFixedThreadPool(poolSize);
	}

	public static void main(String[] args) throws IOException {
		db = new MysqlConn();
		Server server = new Server(PORT, POOL_SIZE);

		// Close all sockets when java program is terminated
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				server.shutDown();
			}
		});

		server.start();
	}

	/**
	 * <p>
	 * <b> Starts the server and creates new threads for necessary classes that need
	 * to listen for data and handle it</b>
	 * </p>
	 * <p>
	 * <b> Also listens for any incoming connection request </b>
	 * </p>
	 * 
	 * @throws IOException
	 */
	private void start() throws IOException {
		System.out.println("Server is listening on port: " + serverSocket.getLocalPort());

		setUpServices();

		while (true) {
			// Accepts any connections
			Socket clientSocket = (Socket) serverSocket.accept();
			System.out.println("Client Connected: " + clientSocket.toString());

			// Add client to existing client list
			ClientHandler cl = new ClientHandler(clientSocket, Integer.toString(proc_ID_Counter));
			proc_ID_Counter++;

			clients.add(cl);
		}
	}

	// Sets up services to run in the background as a daemon thread
	private void setUpServices() {
		// A thread to handle the pinging to the clients every minute or so
		TimerTask pingTask = new PingHandler();
		Timer timer = new Timer(true);
		timer.scheduleAtFixedRate(pingTask, INIT_DELAY, PING_INTERVAL);

		TimerTask listenTask = new ListenForDataHandler();
		Timer timer2 = new Timer(true);
		timer2.scheduleAtFixedRate(listenTask, INIT_DELAY, DATA_LISTEN_INTERVAL);

		// I think for some reason the garbage collector gets rid of the database object
		// and so I have to keep it alive if I want to run it for long periods of time
		TimerTask keepDatabaseAlive = new KeepDatabaseConnAlive();
		Timer timer3 = new Timer(true);
		timer3.scheduleAtFixedRate(keepDatabaseAlive, INIT_DELAY, KEEP_DATABASE_ALIVE_INTERVAL);
	}

	/**
	 * <p>
	 * <b> Closes the server socket so it stops listening when the server stops.
	 * </b>
	 * </p>
	 */
	private void shutDown() {
		System.out.println("Shutting down the server...");
		try {
			serverSocket.close();
			Server.db.dbConn.close();
		} catch (IOException | SQLException e) {
			e.printStackTrace();
		}

	}
}