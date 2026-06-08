package net.p3pp3rf1y.devclientautomation.demo;

import net.minecraft.client.Minecraft;
import net.p3pp3rf1y.devclientautomation.JsonUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DemoSession {
	private static final DemoSession INSTANCE = new DemoSession();

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
		StringBuilder json = new StringBuilder("{\n");
		json.append("  ").append(JsonUtil.property("name", name)).append(",\n");
		json.append("  \"commands\": [\n");
		for (int i = 0; i < commands.size(); i++) {
			json.append("    \"").append(JsonUtil.escape(commands.get(i))).append("\"");
			if (i + 1 < commands.size()) {
				json.append(',');
			}
			json.append('\n');
		}
		json.append("  ]\n");
		json.append("}\n");
		return json.toString();
	}

	private static List<String> parseCommands(String json) {
		List<String> parsedCommands = new ArrayList<>();
		int commandsIndex = json.indexOf("\"commands\"");
		if (commandsIndex < 0) {
			return parsedCommands;
		}
		int arrayStart = json.indexOf('[', commandsIndex);
		int arrayEnd = json.indexOf(']', arrayStart);
		if (arrayStart < 0 || arrayEnd < 0) {
			return parsedCommands;
		}

		String commandsJson = json.substring(arrayStart + 1, arrayEnd);
		int index = 0;
		while (index < commandsJson.length()) {
			int quoteStart = commandsJson.indexOf('"', index);
			if (quoteStart < 0) {
				break;
			}
			StringBuilder command = new StringBuilder();
			boolean escaped = false;
			for (int i = quoteStart + 1; i < commandsJson.length(); i++) {
				char c = commandsJson.charAt(i);
				if (escaped) {
					command.append(switch (c) {
						case 'n' -> '\n';
						case 'r' -> '\r';
						default -> c;
					});
					escaped = false;
				} else if (c == '\\') {
					escaped = true;
				} else if (c == '"') {
					parsedCommands.add(command.toString());
					index = i + 1;
					break;
				} else {
					command.append(c);
				}
			}
		}
		return parsedCommands;
	}

	private static Path demoPath(String demoName) {
		return Minecraft.getInstance().gameDirectory.toPath().resolve("devclientautomation").resolve("demos").resolve(demoName + ".json");
	}
}
