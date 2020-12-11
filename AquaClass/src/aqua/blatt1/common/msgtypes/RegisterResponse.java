package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.util.Calendar;

@SuppressWarnings("serial")
public final class RegisterResponse implements Serializable {
	private final String id;
	private final int leaseTime;

	public RegisterResponse(String id, int leaseTime) {
		this.leaseTime = leaseTime;
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public int getLeaseTime() { return leaseTime; }

}
