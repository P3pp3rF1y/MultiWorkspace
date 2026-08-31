package net.p3pp3rf1y.devclientautomation.demo;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DemoSession {
	private static final DemoSession INSTANCE = new DemoSession();
	private static final Gson GSON = new Gson();

	private String name = "";
	private final List<String> commands = new ArrayList<>();

	public static DemoSession get() {
		return INSTANCE;
	}

	public void start(String demoName) {
		name = demoName;
		commands.clear();
	}

	public boolean isActive() {
		return !name.isEmpty();
	}

	public String getName() {
		return name;
	}

	public void record(String command) {
		if (isActive()) {
			commands.add(command);
		}
	}

	public int size() {
		return commands.size();
	}

	public Path save() throws IOException {
		if (!isActive()) {
			throw new IllegalStateException("No active demo");
		}
		Path path = demoPath(name);
		Files.createDirectories(path.getParent());
		Files.writeString(path, toJson(), StandardCharsets.UTF_8);
		return path;
	}

	public List<String> load(String demoName) throws IOException {
		Path path = demoPath(demoName);
		String json = Files.readString(path, StandardCharsets.UTF_8);
		return parseCommands(json);
	}

	private String toJson() {
		JsonObject demo = new JsonObject();
		demo.addProperty("name", name);
		JsonArray commandArray = new JsonArray();
		commands.forEach(commandArray::add);
		demo.add("commands", commandArray);
		return GSON.toJson(demo);
	}

	private static List<String> parseCommands(String json) {
		JsonObject demo = JsonParser.parseString(json).getAsJsonObject();
		JsonElement commands = demo.get("commands");
		if (commands == null || !commands.isJsonArray()) {
			throw new IllegalArgumentException("Demo commands must be an array");
		}
		return GSON.fromJson(commands, new TypeToken<>() {
		});
	}

	private static Path demoPath(String demoName) {
		return Minecraft.getInstance().gameDirectory.toPath().resolve("devclientautomation").resolve("demos").resolve(demoName + ".json");
	}
}
