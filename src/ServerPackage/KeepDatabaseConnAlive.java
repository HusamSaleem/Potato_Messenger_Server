package ServerPackage;

import java.util.TimerTask;

public class KeepDatabaseConnAlive extends TimerTask {

	@Override
	public void run() {
		if (Server.db.dbConn == null) {
			Server.db.startConnection();
		}

	}
}