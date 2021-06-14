package ServerPackage;

import java.util.TimerTask;

/**
 * <p>
 * <b> Listens for each client's incoming data and executes them in a threadpool
 * </b>
 * </p>
 * 
 * @author Husam Saleem
 */
public class ListenForDataHandler extends TimerTask {

	@Override
	public void run() {
		for (ClientHandler client : Server.clients) {
			if (!client.S.isClosed() && client.isConnected())
				Server.threadPool.execute(client);
		}
	}
}