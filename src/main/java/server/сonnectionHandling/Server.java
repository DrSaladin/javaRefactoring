package server.сonnectionHandling;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
  /** Port number*/
  final int portal = 9999;
  /** Threads pool */
  ExecutorService executorService = Executors.newFixedThreadPool(64);
  /** Connection handler class */
  ConnectionHandling connectionHandling = new ConnectionHandling();

  public void Server() {}

  public void startServer() {
    try (final var serverSocket = new ServerSocket(portal)) {
      while (true) {
        Socket clientSocket = serverSocket.accept();
        executorService.execute(() -> connectionHandling.handleNewClient(clientSocket));
      }
    } catch (
      IOException e) {
      e.printStackTrace();
    }
  }
}
