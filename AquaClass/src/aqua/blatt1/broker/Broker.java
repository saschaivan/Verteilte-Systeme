package aqua.blatt1.broker;

import aqua.blatt1.client.ClientCommunicator;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Broker {
    private ClientCollection<ClientCommunicator> clients = new ClientCollection<>();

    private Endpoint endpoint;

    public Broker() {
        this.endpoint = new Endpoint(4711);
        while(true) {
            Message message = this.endpoint.blockingReceive();
            var payload = message.getPayload();

        }
    }

    private void register(InetSocketAddress addr) {
        ClientCommunicator newClient = new ClientCommunicator();
        String id = "tank" + this.clients.size() + 1;
        this.clients.add("tank" + this.clients.size() + 1, newClient);
        this.endpoint.send(addr, new RegisterResponse(id));
    }

    private void deregister() {

    }

    public static void main(final String[] args) {
        Broker broker = new Broker();
    }
}

