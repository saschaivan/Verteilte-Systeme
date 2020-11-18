package aqua.blatt1.broker;

import aqua.blatt1.aqua.blatt2.broker.PoisonPill;
import aqua.blatt1.aqua.blatt2.broker.Poisoner;
import aqua.blatt1.client.ClientCommunicator;
import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.msgtypes.*;
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
    private int numThreads = 6;
    private int counter;
    private volatile boolean stopRequested = false;

    ExecutorService executerService;

    public Broker() {
        endpoint = new Endpoint(Properties.PORT);
        clients = new ClientCollection<>();
        executerService = Executors.newFixedThreadPool(numThreads);
        counter = 0;
    }

    private void broker() {
        Thread stopServerThread = new Thread(() -> {
            int a = JOptionPane.showOptionDialog(null,
                    "Press ok to stop Server",
                    "",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                    null,
                    null,
                    null);
            if (a == 0) {
                stopRequested = true;
                System.exit(0);
            }
        });
        stopServerThread.start();
        while(!stopRequested) {
            var message = endpoint.blockingReceive();
            var brokerTask = new BrokerTask(message);
            executerService.execute(brokerTask);
        }
        System.exit(0);
        executerService.shutdown();
    }


    public static void main(final String[] args) {
        Broker broker = new Broker();
        broker.broker();
    }

    public class BrokerTask implements Runnable {
        ReadWriteLock lock;
        Message message;

        public BrokerTask(Message message) {
            lock = new ReentrantReadWriteLock();
            this.message = message;
        }

        @Override
        public void run() {
            var payload = message.getPayload();
            if (payload instanceof RegisterRequest)
                register(message);
            if (payload instanceof DeregisterRequest)
                deregister(message);
            if (payload instanceof HandoffRequest)
                handoffFish(message);
            if (payload instanceof PoisonPill)
                stopRequested = true;
        }

        private void notifyNeighbors(InetSocketAddress address) {
            InetSocketAddress leftNeighbor = clients.getLeftNeighorOf(clients.indexOf(address));
            InetSocketAddress rightNeighbor = clients.getRightNeighorOf(clients.indexOf(address));
            endpoint.send(address, new NeighborUpdate(leftNeighbor, Direction.LEFT));
            endpoint.send(address, new NeighborUpdate(rightNeighbor, Direction.RIGHT));
            endpoint.send(leftNeighbor, new NeighborUpdate(address, Direction.RIGHT));
            endpoint.send(rightNeighbor, new NeighborUpdate(address, Direction.LEFT));
        }

        private void register(Message message) {
            var id = "tank" + counter++;
            var sender = message.getSender();
            lock.writeLock().lock();
            clients.add(id, sender);
            lock.writeLock().unlock();
            endpoint.send(sender, new RegisterResponse(id));
            notifyNeighbors(sender);
            if (clients.size() == 1)
                endpoint.send(sender, new Token());
        }

        private void deregister(Message message) {
            var deregisterrequest = (DeregisterRequest) message.getPayload();
            var clientid = deregisterrequest.getId();
            var client = clients.indexOf(clientid);
            lock.writeLock().lock();
            clients.remove(client);
            lock.writeLock().unlock();
            if(clients.size() > 1) {
                InetSocketAddress leftNeighbor = clients.getLeftNeighorOf(clients.indexOf(message.getSender()));
                InetSocketAddress rightNeighbor = clients.getRightNeighorOf(clients.indexOf(message.getSender()));
                endpoint.send(leftNeighbor, new NeighborUpdate(rightNeighbor, Direction.RIGHT));
                endpoint.send(rightNeighbor, new NeighborUpdate(leftNeighbor, Direction.LEFT));
            }
        }

        private void handoffFish(Message message) {
            var handoffRequest = (HandoffRequest) message.getPayload();
            InetSocketAddress receiver;
            FishModel fish = handoffRequest.getFish();
            lock.readLock().lock();
            var tankindex = clients.indexOf(message.getSender());
            if (fish.getDirection() == Direction.LEFT)
                receiver = clients.getLeftNeighorOf(tankindex);
            else
                receiver = clients.getRightNeighorOf(tankindex);
            lock.readLock().unlock();
            endpoint.send(receiver, handoffRequest);
        }
    }
}

