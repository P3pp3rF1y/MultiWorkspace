package net.p3pp3rf1y.devclientautomation.bridge;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public final class AutomationBridge {
	private final Logger logger;
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, runnable -> {
		Thread thread = new Thread(runnable, "Dev Client Automation");
		thread.setDaemon(true);
		return thread;
	});

	public AutomationBridge(Logger logger) {
		this.logger = logger;
	}

	public void start(Consumer<EndpointRegistry> endpointRegistrar, Path gameDirectory) {
		try {
			HttpServer httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
			endpointRegistrar.accept(new EndpointRegistry(httpServer));
			httpServer.setExecutor(executor);
			httpServer.start();
			writeDiscoveryFile(gameDirectory, httpServer.getAddress().getPort());
			logger.info("Dev client automation bridge started on 127.0.0.1:{}", httpServer.getAddress().getPort());
		} catch (IOException e) {
			logger.error("Failed to start dev client automation bridge", e);
		}
	}

	private void writeDiscoveryFile(Path gameDirectory, int port) {
		Path discoveryFile = gameDirectory.resolve("dev-client-automation.json");
		String json = "{\"host\":\"127.0.0.1\",\"port\":" + port + ",\"processId\":" + ProcessHandle.current().pid() + "}";
		try {
			Files.writeString(discoveryFile, json, StandardCharsets.UTF_8);
		} catch (IOException e) {
			logger.warn("Failed to write dev client automation discovery file {}", discoveryFile, e);
		}
	}
}
