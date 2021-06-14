package ServerPackage;

import java.util.Iterator;
import java.util.TimerTask;

/**
 * <p>
 * <b> Pings all clients to make sure that there are no sockets opened for no
 * reason </b>
 * </p>
 * 
 * @author Husam Saleem
 */
public class PingHandler extends TimerTask {

	// The interval for when pings go out to clients,
	private final long PING_INTERVAL = 60000;
	private final long GRACE_PERIOD = 500; // 500 milisecond grace period

	@Override
	public void run() {
		Iterator<ClientHandler> iter = Server.clients.iterator();

		while (iter.hasNext()) {
			ClientHandler client = iter.next();

			// If the client is not connected anymore -> close the connection
			if (!client.isConnected()) {
				System.out.println("Client has been removed: " + client.S.toString());
				/*
				 * try { client.S.close(); } catch (IOException e) { e.printStackTrace(); }
				 */
				iter.remove();
				continue;
			}

			client.sendData("Ping!");

			// If the client exceeded the maximum amount of times they could be pinged with
			// no response -> close the connection
			if ((System.currentTimeMillis() - (client.getLastPingTime() + GRACE_PERIOD)) > PING_INTERVAL) {
				client.increaseRetryCount();

				if (!client.keepAlive() || !client.isConnected()) {
					System.out.println("Client has been removed: " + client.S.toString());
					iter.remove();

					/*
					 * try { client.S.close(); } catch (IOException e) { e.printStackTrace(); }
					 */
				}
			}

		}

		System.out.println("Finished pinging clients...");
	}
}