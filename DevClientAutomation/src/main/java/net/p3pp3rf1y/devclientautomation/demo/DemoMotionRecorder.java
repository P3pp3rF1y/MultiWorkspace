package net.p3pp3rf1y.devclientautomation.demo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.p3pp3rf1y.devclientautomation.JsonUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DemoMotionRecorder {
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
		if (!isRecording()) {
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
		StringBuilder json = new StringBuilder("{\n");
		json.append("  ").append(JsonUtil.property("name", name)).append(",\n");
		json.append("  \"samples\": [\n");
		for (int i = 0; i < SAMPLES.size(); i++) {
			json.append("    ").append(SAMPLES.get(i).toJson());
			if (i + 1 < SAMPLES.size()) {
				json.append(',');
			}
			json.append('\n');
		}
		json.append("  ]\n");
		json.append("}\n");
		return json.toString();
	}

	private static Path motionPath(String name) {
		return Minecraft.getInstance().gameDirectory.toPath().resolve("devclientautomation").resolve("motion-recordings").resolve(name + ".json");
	}

	public record ActionDebug(String action, String phase, int tick, int maxTicks, double remainingDistance, double moveTargetX, double moveTargetY,
			double moveTargetZ, double lookTargetX, double lookTargetY, double lookTargetZ, float cameraYaw, float cameraPitch, float targetYaw,
			float targetPitch, float movementReferenceYaw) {
		private String toJson() {
			return String.format(Locale.ROOT,
					"{\"action\":\"%s\",\"phase\":\"%s\",\"tick\":%d,\"maxTicks\":%d,\"remainingDistance\":%.5f,\"moveTarget\":{\"x\":%.5f,\"y\":%.5f,\"z\":%.5f},\"lookTarget\":{\"x\":%.5f,\"y\":%.5f,\"z\":%.5f},\"cameraYaw\":%.3f,\"cameraPitch\":%.3f,\"targetYaw\":%.3f,\"targetPitch\":%.3f,\"movementReferenceYaw\":%.3f}",
					JsonUtil.escape(action), JsonUtil.escape(phase), tick, maxTicks, remainingDistance, moveTargetX, moveTargetY, moveTargetZ, lookTargetX,
					lookTargetY, lookTargetZ, cameraYaw, cameraPitch, targetYaw, targetPitch, movementReferenceYaw);
		}
	}

	private record Sample(double timeMs, double x, double y, double z, float yaw, float pitch, float yawDelta, float pitchDelta, boolean forward, boolean back,
			boolean left, boolean right, boolean jump, boolean sneak, boolean sprint, ActionDebug actionDebug) {
		private String toJson() {
			return String.format(Locale.ROOT,
					"{\"timeMs\":%.3f,\"x\":%.5f,\"y\":%.5f,\"z\":%.5f,\"yaw\":%.3f,\"pitch\":%.3f,\"yawDelta\":%.3f,\"pitchDelta\":%.3f,\"keys\":{\"forward\":%s,\"back\":%s,\"left\":%s,\"right\":%s,\"jump\":%s,\"sneak\":%s,\"sprint\":%s},\"action\":%s}",
					timeMs, x, y, z, yaw, pitch, yawDelta, pitchDelta, forward, back, left, right, jump, sneak, sprint,
					actionDebug == null ? "null" : actionDebug.toJson());
		}
	}
}
