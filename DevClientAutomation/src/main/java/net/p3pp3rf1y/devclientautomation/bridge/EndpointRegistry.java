package net.p3pp3rf1y.devclientautomation.bridge;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public final class EndpointRegistry {
	private final HttpServer httpServer;

	public EndpointRegistry(HttpServer httpServer) {
		this.httpServer = httpServer;
	}

	public void register(String path, HttpHandler handler) {
		httpServer.createContext(path, handler);
	}
}
