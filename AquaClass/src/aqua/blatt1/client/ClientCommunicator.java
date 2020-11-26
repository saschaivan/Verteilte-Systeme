package aqua.blatt1.client;

import java.net.InetSocketAddress;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;

public class ClientCommunicator {
	private final Endpoint endpoint;

	public ClientCommunicator() {
		endpoint = new Endpoint();
	}

	public class ClientForwarder {
		private final InetSocketAddress broker;

		private ClientForwarder() {
			this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
		}

		public void register() {
			endpoint.send(broker, new RegisterRequest());
		}

		public void deregister(String id) {
			endpoint.send(broker, new DeregisterRequest(id));
		}

		public void handOff(FishModel fish, InetSocketAddress address) {
			endpoint.send(address, new HandoffRequest(fish));
		}

		public void handOffToken(Token token, InetSocketAddress address) {
			endpoint.send(address, token);
		}

		public void sendSnapshotMarker(InetSocketAddress address) {
			endpoint.send(address, new SnapshotMarker());
		}

		public void sendSnapshotCollectionMarker(InetSocketAddress address, CollectSnapshot cs) {
			endpoint.send(address, cs);
		}
	}

	public class ClientReceiver extends Thread {
		private final TankModel tankModel;

		private ClientReceiver(TankModel tankModel) {
			this.tankModel = tankModel;
		}

		@Override
		public void run() {
			while (!isInterrupted()) {
				Message msg = endpoint.blockingReceive();

				if (msg.getPayload() instanceof RegisterResponse)
					tankModel.onRegistration(((RegisterResponse) msg.getPayload()).getId());

				if (msg.getPayload() instanceof HandoffRequest)
					tankModel.receiveFish(((HandoffRequest) msg.getPayload()).getFish());

				if(msg.getPayload() instanceof NeighborUpdate) {
					InetSocketAddress address = ((NeighborUpdate) msg.getPayload()).getAddress();
					Direction direction = ((NeighborUpdate) msg.getPayload()).getDirection();

					if (direction == Direction.LEFT)
						tankModel.setLeftNeighbor(address);

					if (direction == Direction.RIGHT)
						tankModel.setRightNeighbor(address);

				}

				if (msg.getPayload() instanceof Token)
					tankModel.receiveToken((Token) msg.getPayload());

				if (msg.getPayload() instanceof SnapshotMarker) {
					if (msg.getSender().equals(tankModel.leftNeighbor))
						tankModel.handleReceivedMarker("left");
					else
						tankModel.handleReceivedMarker("right");
				}

				if (msg.getPayload() instanceof CollectSnapshot) {
					tankModel.hasSnapshotCollectToken = true;
					tankModel.snapshotCollector = (CollectSnapshot) msg.getPayload();
					if (tankModel.isInitiator) {
						tankModel.isSnapshotDone = true;
						tankModel.hasSnapshotCollectToken = false;
						tankModel.isInitiator = false;
					} else {
						tankModel.hasSnapshotCollectToken = false;
						tankModel.snapshotCollector.addFishies(tankModel.localState);
						tankModel.forwarder.sendSnapshotCollectionMarker(tankModel.leftNeighbor, tankModel.snapshotCollector);
					}

				}
			}
			System.out.println("Receiver stopped.");
		}
	}

	public ClientForwarder newClientForwarder() {
		return new ClientForwarder();
	}

	public ClientReceiver newClientReceiver(TankModel tankModel) {
		return new ClientReceiver(tankModel);
	}

}
