package backend.CentralServer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.logging.Logger;

@Component
public class Server {
    private int id;
    private boolean coordinator; //TODO: should the coordinator be the proxy???
    private final int port;
    private final Logger log;

    private int numberOfRooms;

    // Constructor, getters, and setters

    public Server(int port, int id, boolean isCoordinator) {
        this.port = port;
        this.log = Logger.getLogger(ServerApplication.class.getName() + "-port:" + port);
        this.id = id;
        this.coordinator = isCoordinator;
    }

    public void initiateElection(List<Server> servers) {
        System.out.println("Server " + id + " is initiating an election");
        for (Server server : servers) {
            if (server.getId() > id) {
                try {
                    server.receiveElection(id);
                } catch (Exception e) {
                    // Handle unreachable server
                    e.printStackTrace();
                }
            }
        }
        setCoordinator(true);
        System.out.println("Server " + id + " becomes the coordinator");
    }

    public void receiveElection(int senderId) {
        System.out.println("Server " + id + " received election message from Server " + senderId);
        if (id > senderId) {
            System.out.println("Server " + id + " has higher priority. Sending OK message to Server " + senderId);
            sendOk(senderId);
        }
    }

    //TODO: will need to modify
    public void sendOk(int receiverId) {
        System.out.println("Server " + id + " sends OK message to Server " + receiverId);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isCoordinator() {
        return coordinator;
    }

    public void setCoordinator(boolean coordinator) {
        this.coordinator = coordinator;
    }
}