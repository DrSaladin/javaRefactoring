package server.сonnectionHandling;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import utils.RequestInfo;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class ConnectionHandling {
  final List<String> validPaths = List.of(
    "/index",
    "/spring.svg",
    "/spring.png",
    "/resources",
    "/styles.css",
    "/app.js",
    "/links",
    "/forms",
    "/classic",
    "/events",
    "/events.js"
  );
  RequestInfo currentRequestInfo;
  /* List of methods */
  enum RequestMethod {
    GET,
    POST,
    PUT,
    DELETE
  }
  private String path = null;
  public ConnectionHandling() {}

  /* Get request info, including path and params */
  private RequestInfo getCurrentRequestInfo(String requestLine) {
    var parts = requestLine.split(" ");
    RequestInfo requestInfo = new RequestInfo();

    if (!requestLine.isEmpty()) {
      String url = parts[1];
      requestInfo.setMethod(parts[0]);
      int queryStart = url.indexOf('?');

      if (queryStart >= 0) {
        String path = url.substring(0, queryStart);
        String queryString = url.substring(queryStart + 1);

        requestInfo.setParamsList(URLEncodedUtils.parse(queryString, StandardCharsets.UTF_8));
        requestInfo.setRequestPath(path);
      } else {
        requestInfo.setRequestPath(url);
        requestInfo.setParamsList(null);
      }
    }
    return requestInfo;
  }

  private String getMethodType(String requestLine) {
    var parts = requestLine.split(" ");
    return parts[0];
  }

  private void handleWrongPath(BufferedOutputStream out) throws IOException {
      out.write((
        "HTTP/1.1 404 Not Found\r\n" +
        "Content-Length: 0\r\n" +
        "Connection: close\r\n" +
        "\r\n"
      ).getBytes());
      out.flush();
  }

  private List<NameValuePair> getBodyData(BufferedReader in) throws IOException {
    String line;
    int contentLength = 0;
    List<NameValuePair> params = null;

    while (!(line = in.readLine()).isEmpty()) {
      if (line.startsWith("Content-Length: ")) {
        contentLength = Integer.parseInt(line.substring("Content-Length: ".length()));
      }
    }

    if (contentLength > 0) {
      char[] bodyChars = new char[contentLength];
      in.read(bodyChars, 0, contentLength);
      String body = new String(bodyChars);
      params = URLEncodedUtils.parse(body, StandardCharsets.UTF_8);
    }

    return params;
  }

  private void handleRequests(String bufferedReadeLine, BufferedReader in) {
    try {
      RequestMethod method = RequestMethod.valueOf(getMethodType(bufferedReadeLine));
      if (method == RequestMethod.GET) {
        currentRequestInfo = getCurrentRequestInfo(bufferedReadeLine);
        path = currentRequestInfo.getRequestPath();
      } else if (method == RequestMethod.POST) {
        currentRequestInfo = getCurrentRequestInfo(bufferedReadeLine);
        currentRequestInfo.setParamsList(getBodyData(in));
        path = currentRequestInfo.getRequestPath();
      }
    } catch (IllegalArgumentException | IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private void handleClassicRequest(Path filePath, BufferedOutputStream out, String mimeType) throws IOException {
    final var template = Files.readString(filePath);
    final var content = template.replace(
            "{time}",
            LocalDateTime.now().toString()
    ).getBytes();
    out.write((
      "HTTP/1.1 200 OK\r\n" +
      "Content-Type: " + mimeType + "\r\n" +
      "Content-Length: " + content.length + "\r\n" +
      "Connection: close\r\n" +
      "\r\n"
    ).getBytes());
    out.write(content);
    out.flush();
  }

  private void handleFileRequest(Path filePath, BufferedOutputStream out, String mimeType) throws IOException {
    final var length = Files.size(filePath);
    out.write((
      "HTTP/1.1 200 OK\r\n" +
      "Content-Type: " + mimeType + "\r\n" +
      "Content-Length: " + length + "\r\n" +
      "Connection: close\r\n" +
      "\r\n"
    ).getBytes());
    Files.copy(filePath, out);
    out.flush();
  }

  public void handleNewClient(Socket clientSocket) {
    new Thread(() -> {
      try (
        final var in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        final var out = new BufferedOutputStream(clientSocket.getOutputStream());
      ) {
        String bufferedReadeLine = in.readLine();

        if(bufferedReadeLine.length() == 0) return;

        handleRequests(bufferedReadeLine, in);

        if(!validPaths.contains(path)) {
          handleWrongPath(out);
          return;
        }

        final var filePath = Path.of(".", "public", path);
        final var mimeType = Files.probeContentType(filePath);
        if (path.equals("/classic")) {
          handleClassicRequest(filePath, out, mimeType);
          return;
        }

        if(Files.exists(filePath)) {
          handleFileRequest(filePath, out, mimeType);
        }

      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }).start();
  }
}
