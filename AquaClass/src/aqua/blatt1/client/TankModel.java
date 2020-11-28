package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.broker.Broker;
import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.CollectSnapshot;
import aqua.blatt1.common.msgtypes.Token;

public class TankModel extends Observable implements Iterable<FishModel> {
	public static final int WIDTH = 600;
	public static final int HEIGHT = 350;
	protected static final int MAX_FISHIES = 5;
	protected static final Random rand = new Random();
	protected volatile String id;
	protected final Set<FishModel> fishies;
	protected int fishCounter = 0;
	protected final ClientCommunicator.ClientForwarder forwarder;
	protected InetSocketAddress rightNeighbor;
	protected InetSocketAddress leftNeighbor;
	protected boolean hasToken;
	protected int localState;
	protected State state = State.IDLE;
	protected boolean isInitiator;
	protected volatile boolean hasSnapshotCollectToken;
	protected volatile CollectSnapshot snapshotCollector;
	protected volatile boolean isSnapshotDone;
	protected int fadingFishCounter;
	protected Map<String, InetSocketAddress> homeAgent = new TreeMap<>();

	public void setRightNeighbor(InetSocketAddress address) {
		this.rightNeighbor = address;
	}

	public void setLeftNeighbor(InetSocketAddress address) {
		this.leftNeighbor = address;
	}


	public TankModel(ClientCommunicator.ClientForwarder forwarder) {
		this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
		this.forwarder = forwarder;
	}

	synchronized void onRegistration(String id) {
		this.id = id;
		newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
	}

	public synchronized void newFish(int x, int y) {
		if (fishies.size() < MAX_FISHIES) {
			x = x > WIDTH - FishModel.getXSize() - 1 ? WIDTH - FishModel.getXSize() - 1 : x;
			y = y > HEIGHT - FishModel.getYSize() ? HEIGHT - FishModel.getYSize() : y;

			FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
					rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

			fishies.add(fish);

			homeAgent.put(fish.getId(), null);
		}
	}

	synchronized void receiveFish(FishModel fish) {
		fish.setToStart();
		fishies.add(fish);
		localState++;
		if (!homeAgent.containsKey(fish.getId()))
			forwarder.sendNameResolutionRequest(fish.getTankId(), fish.getId());
		else
			homeAgent.put(fish.getId(), null);
	}

	public String getId() {
		return id;
	}

	public synchronized int getFishCounter() {
		return fishCounter;
	}

	public synchronized Iterator<FishModel> iterator() {
		return fishies.iterator();
	}

	private synchronized void updateFishies() {
		for (Iterator<FishModel> it = iterator(); it.hasNext();) {
			FishModel fish = it.next();

			fish.update();

			if (fish.hitsEdge()) {
				if (!hasToken) {
					fish.reverse();
				} else {
					if (fish.getDirection() == Direction.LEFT) {
						forwarder.handOff(fish, leftNeighbor);
						fadingFishCounter++;
					}
					if (fish.getDirection() == Direction.RIGHT) {
						forwarder.handOff(fish, rightNeighbor);
						fadingFishCounter++;
					}
				}
			}
			if (fish.disappears()) {
				it.remove();
				fadingFishCounter--;
			}
		}
	}

	private synchronized void update() {
		updateFishies();
		setChanged();
		notifyObservers();
	}

	protected void run() {
		forwarder.register();

		try {
			while (!Thread.currentThread().isInterrupted()) {
				update();
				TimeUnit.MILLISECONDS.sleep(10);
			}
		} catch (InterruptedException consumed) {
			// allow method to terminate
		}
	}

	public synchronized void finish() {
		forwarder.deregister(id);
	}

	public void receiveToken(Token token) {
		hasToken = true;
		Timer timer = new Timer();
		TimerTask timertask = new TimerTask() {
			@Override
			public void run() {
				hasToken = false;
				forwarder.handOffToken(token, leftNeighbor);
			}
		};
		timer.schedule(timertask, 2000);
	}

	public boolean hasToken() {
		return hasToken;
	}

	public void initiateSnapshot() {
		this.isInitiator = true;
		this.localState = this.fishies.size() - fadingFishCounter;
		this.state = State.BOTH;
		this.snapshotCollector = new CollectSnapshot();
		this.hasSnapshotCollectToken = true;
		this.forwarder.sendSnapshotMarker(this.leftNeighbor);
		this.forwarder.sendSnapshotMarker(this.rightNeighbor);
	}

	protected void handleReceivedMarker(String dir) {
		State direction;
		if (dir.equals("left"))
			direction = State.RIGHT;
		else
			direction = State.LEFT;

		if (this.state == State.IDLE) {
			this.localState = this.fishies.size() - fadingFishCounter;
			this.state = direction;
			this.forwarder.sendSnapshotMarker(this.leftNeighbor);
			this.forwarder.sendSnapshotMarker(this.rightNeighbor);
		} else {
			if (this.state == State.BOTH) {
				this.state = direction;
			} else {
				this.state = State.IDLE;
				if (hasSnapshotCollectToken) {
					this.hasSnapshotCollectToken = false;
					this.snapshotCollector.addFishies(this.localState);
					forwarder.sendSnapshotCollectionMarker(this.leftNeighbor, this.snapshotCollector);
					this.snapshotCollector = null;
				}
			}
		}
	}

	protected void locateFishGlobally(String fishID) {
		InetSocketAddress address = homeAgent.get(fishID);
		if (address == null)
			locateFishLocally(fishID);
		else
			forwarder.sendLocationRequest(fishID, address);
	}

	protected void locateFishLocally(String fishID) {
		Iterator<FishModel> it = iterator();
		while (it.hasNext()) {
			FishModel fish = it.next();
			if (fish.getId().equals(fishID))
				fish.toggle();
		}
	}

	protected enum State {
		IDLE, LEFT, RIGHT, BOTH
	}
}