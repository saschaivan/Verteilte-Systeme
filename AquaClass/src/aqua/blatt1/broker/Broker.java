package aqua.blatt1.broker;

import aqua.blatt1.client.ClientCommunicator;
import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Broker {
    private ClientCollection<InetSocketAddress> clients;

    private Endpoint endpoint;

    private int counter;

    public Broker() {
        this.endpoint = new Endpoint(Properties.PORT);
        this.clients = new ClientCollection<>();
    }

    private void broker() {
        while(true) {
            var message = this.endpoint.blockingReceive();
            var payload = message.getPayload();
            if (payload instanceof RegisterRequest)
                register(message);

            if (payload instanceof DeregisterRequest)
                deregister(message);

            if (payload instanceof HandoffRequest)
                handoffFish(message);
        }
    }

    private void register(Message message) {
        var id = "tank" + counter++;
        var sender = message.getSender();
        clients.add(id, sender);
        endpoint.send(sender, new RegisterResponse(id));
    }

    private void deregister(Message message) {
        var deregisterrequest = (DeregisterRequest) message.getPayload();
        var clientid = deregisterrequest.getId();
        var client = clients.indexOf(clientid);
        clients.remove(client);
    }

    private void handoffFish(Message message) {
        var handoffRequest = (HandoffRequest) message.getPayload();
        InetSocketAddress receiver;
        FishModel fish = handoffRequest.getFish();
        var tankindex = clients.indexOf(message.getSender());
        if (fish.getDirection() == Direction.LEFT)
            receiver = clients.getLeftNeighorOf(tankindex);
        else
            receiver = clients.getRightNeighorOf(tankindex);

        endpoint.send(receiver, handoffRequest);
    }

    public static void main(final String[] args) {
        Broker broker = new Broker();
        broker.broker();
    }
}

