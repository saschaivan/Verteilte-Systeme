package aqua.blatt1.broker;

import aqua.blatt1.client.ClientCommunicator;
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
    private ClientCollection<ClientCommunicator> clients;

    private Endpoint endpoint;

    public Broker() {
        this.endpoint = new Endpoint(Properties.PORT);
        this.clients = new ClientCollection<>();
    }

    private void broker() {
        while(true) {
            var message = this.endpoint.blockingReceive();
            var payload = message.getPayload();
            if (payload instanceof RegisterRequest)
                register(message.getSender());

            if (payload instanceof DeregisterRequest)
                deregister(this.clients.indexOf(message.getSender().getHostName()));

            if (payload instanceof HandoffRequest)
                handoffFisch();
        }
    }

    private void register(InetSocketAddress addr) {
        ClientCommunicator newClient = new ClientCommunicator();
        var id = "tank" + this.clients.size() + 1;
        this.clients.add("tank" + this.clients.size() + 1, newClient);
        this.endpoint.send(addr, new RegisterResponse(id));
    }

    private void deregister(int id) {
        this.clients.remove(id);
    }

    private void handoffFisch() {

    }

    public static void main(final String[] args) {
        Broker broker = new Broker();
        broker.broker();
    }
}

