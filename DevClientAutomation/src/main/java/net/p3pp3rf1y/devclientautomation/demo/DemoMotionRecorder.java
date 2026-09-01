package net.p3pp3rf1y.devclientautomation.demo;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DemoMotionRecorder {
	private static final int MAX_SAMPLES = 36_000;
	private static final Gson GSON = new Gson();
	private static final List<Sample> SAMPLES = new ArrayList<>();
	private static String recordingName = "";
	private static long startNanos;
	private static float lastYaw;
	private static float lastPitch;
	private static boolean initializedLastRotation;
	private static ActionDebug actionDebug;

	private DemoMotionRecorder() {
	}

	public static void init() {
		NeoForge.EVENT_BUS.addListener(DemoMotionRecorder::recordFrame);
	}

	public static boolean isRecording() {
		return !recordingName.isEmpty();
	}

	public static String recordingName() {
		return recordingName;
	}

	public static double recordingDurationSeconds() {
		return isRecording() ? (System.nanoTime() - startNanos) / 1_000_000_000D : 0D;
	}

	public static int sampleCount() {
		return SAMPLES.size();
	}

	public static void start(String name) {
		recordingName = name;
		SAMPLES.clear();
		startNanos = System.nanoTime();
		initializedLastRotation = false;
		actionDebug = null;
	}

	public static void setActionDebug(ActionDebug debug) {
		actionDebug = debug;
	}

	public static void clearActionDebug() {
		actionDebug = null;
	}

	public static Path stop() throws IOException {
		if (!isRecording()) {
			throw new IllegalStateException("No active motion recording");
		}

		String name = recordingName;
		recordingName = "";
		Path path = motionPath(name);
		Files.createDirectories(path.getParent());
		Files.writeString(path, toJson(name), StandardCharsets.UTF_8);
		return path;
	}

	private static void recordFrame(ViewportEvent.ComputeCameraAngles event) {
		if (!isRecording() || SAMPLES.size() >= MAX_SAMPLES) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null) {
			return;
		}

		float yaw = event.getYaw();
		float pitch = event.getPitch();
		float yawDelta = initializedLastRotation ? wrapDegrees(yaw - lastYaw) : 0F;
		float pitchDelta = initializedLastRotation ? pitch - lastPitch : 0F;
		initializedLastRotation = true;
		lastYaw = yaw;
		lastPitch = pitch;

		SAMPLES.add(new Sample((System.nanoTime() - startNanos) / 1_000_000D, player.getX(), player.getY(), player.getZ(), yaw, pitch, yawDelta, pitchDelta,
				minecraft.options.keyUp.isDown(), minecraft.options.keyDown.isDown(), minecraft.options.keyLeft.isDown(), minecraft.options.keyRight.isDown(),
				minecraft.options.keyJump.isDown(), minecraft.options.keyShift.isDown(), minecraft.options.keySprint.isDown(), actionDebug));
	}

	private static float wrapDegrees(float degrees) {
		float wrapped = degrees % 360F;
		if (wrapped >= 180F) {
			wrapped -= 360F;
		}
		if (wrapped < -180F) {
			wrapped += 360F;
		}
		return wrapped;
	}

	private static String toJson(String name) {
		JsonObject recording = new JsonObject();
		recording.addProperty("name", name);
		JsonArray samples = new JsonArray();
		SAMPLES.forEach(sample -> samples.add(sample.toJson()));
		recording.add("samples", samples);
		return GSON.toJson(recording);
	}

	private static Path motionPath(String name) {
		return Minecraft.getInstance().gameDirectory.toPath().resolve("devclientautomation").resolve("motion-recordings").resolve(name + ".json");
	}

	public record ActionDebug(String action, String phase, int tick, int maxTicks, double remainingDistance, double moveTargetX, double moveTargetY,
			double moveTargetZ, double lookTargetX, double lookTargetY, double lookTargetZ, float cameraYaw, float cameraPitch, float targetYaw,
			float targetPitch, float movementReferenceYaw) {
		private JsonObject toJson() {
			JsonObject debug = new JsonObject();
			debug.addProperty("action", action);
			debug.addProperty("phase", phase);
			debug.addProperty("tick", tick);
			debug.addProperty("maxTicks", maxTicks);
			debug.addProperty("remainingDistance", remainingDistance);
			JsonObject moveTarget = new JsonObject();
			moveTarget.addProperty("x", moveTargetX);
			moveTarget.addProperty("y", moveTargetY);
			moveTarget.addProperty("z", moveTargetZ);
			debug.add("moveTarget", moveTarget);
			JsonObject lookTarget = new JsonObject();
			lookTarget.addProperty("x", lookTargetX);
			lookTarget.addProperty("y", lookTargetY);
			lookTarget.addProperty("z", lookTargetZ);
			debug.add("lookTarget", lookTarget);
			debug.addProperty("cameraYaw", cameraYaw);
			debug.addProperty("cameraPitch", cameraPitch);
			debug.addProperty("targetYaw", targetYaw);
			debug.addProperty("targetPitch", targetPitch);
			debug.addProperty("movementReferenceYaw", movementReferenceYaw);
			return debug;
		}
	}

	private record Sample(double timeMs, double x, double y, double z, float yaw, float pitch, float yawDelta, float pitchDelta, boolean forward, boolean back,
			boolean left, boolean right, boolean jump, boolean sneak, boolean sprint, ActionDebug actionDebug) {
		private JsonObject toJson() {
			JsonObject sample = new JsonObject();
			sample.addProperty("timeMs", timeMs);
			sample.addProperty("x", x);
			sample.addProperty("y", y);
			sample.addProperty("z", z);
			sample.addProperty("yaw", yaw);
			sample.addProperty("pitch", pitch);
			sample.addProperty("yawDelta", yawDelta);
			sample.addProperty("pitchDelta", pitchDelta);
			JsonObject keys = new JsonObject();
			keys.addProperty("forward", forward);
			keys.addProperty("back", back);
			keys.addProperty("left", left);
			keys.addProperty("right", right);
			keys.addProperty("jump", jump);
			keys.addProperty("sneak", sneak);
			keys.addProperty("sprint", sprint);
			sample.add("keys", keys);
			sample.add("action", actionDebug == null ? JsonNull.INSTANCE : actionDebug.toJson());
			return sample;
		}
	}
}
