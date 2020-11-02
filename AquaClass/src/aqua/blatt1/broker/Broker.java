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
import javax.swing.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Broker {
    private ClientCollection<InetSocketAddress> clients;
    private Endpoint endpoint;
    private int numThreads = 10;
    private int counter;
    private volatile boolean stopRequested = false;
    //private Poisoner poisoner;

    ExecutorService executerService;
    ReadWriteLock lock = new ReentrantReadWriteLock();

    public Broker() {
        endpoint = new Endpoint(Properties.PORT);
        clients = new ClientCollection<>();
        executerService = Executors.newFixedThreadPool(numThreads);
    }

    private void broker() {
        if (!stopRequested) {
            while(true) {
                var brokerTask = new BrokerTask();
                executerService.execute(brokerTask);
            }
        }
        executerService.shutdown();
    }

    public static void main(final String[] args) {
        Broker broker = new Broker();
        broker.broker();
    }

    public class BrokerTask implements Runnable {
        @Override
        public void run() {
            var message = endpoint.blockingReceive();
            var payload = message.getPayload();
            if (payload instanceof RegisterRequest)
                register(message);

            if (payload instanceof DeregisterRequest)
                deregister(message);

            if (payload instanceof HandoffRequest)
                handoffFish(message);
        }

        private void register(Message message) {
            lock.writeLock().lock();
            var id = "tank" + counter++;
            var sender = message.getSender();
            clients.add(id, sender);
            endpoint.send(sender, new RegisterResponse(id));
            lock.writeLock().unlock();
        }

        private void deregister(Message message) {
            lock.writeLock().lock();
            var deregisterrequest = (DeregisterRequest) message.getPayload();
            var clientid = deregisterrequest.getId();
            var client = clients.indexOf(clientid);
            clients.remove(client);
            lock.writeLock().unlock();
        }

        private void handoffFish(Message message) {
            lock.readLock().lock();
            var handoffRequest = (HandoffRequest) message.getPayload();
            InetSocketAddress receiver;
            FishModel fish = handoffRequest.getFish();
            var tankindex = clients.indexOf(message.getSender());
            if (fish.getDirection() == Direction.LEFT)
                receiver = clients.getLeftNeighorOf(tankindex);
            else
                receiver = clients.getRightNeighorOf(tankindex);

            endpoint.send(receiver, handoffRequest);
            lock.readLock().unlock();
        }
    }
}

