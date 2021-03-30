package com.google.devtools.build.lib.bazel.bzlmod;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.rules.ExternalResource;

public class TestHttpServer extends ExternalResource {

  private HttpServer server;

  @Override
  protected void before() throws Throwable {
    server = HttpServer.create(new InetSocketAddress(0), 0);
  }

  @Override
  protected void after() {
    server.stop(0);
  }

  public void start() {
    server.start();
  }

  public void serve(String path, byte[] bytes) {
    server.createContext(path, exchange -> {
      exchange.sendResponseHeaders(200, bytes.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(bytes);
      }
    });
  }

  public void serve(String path, String content) {
    serve(path, content.getBytes());
  }

  public String getUrl() throws MalformedURLException {
    return new URL("http", server.getAddress().getHostString(), server.getAddress().getPort(), "")
        .toString();
  }
}
