package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Screenshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class CaptureScreenshotAction extends TypedRegressionAction<String> {
	public CaptureScreenshotAction() {
		super("diagnostic.screenshot", "1", Set.of(CLIENT_CAPABILITY));
	}

	@Override
	protected String parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("name"), description);
		String name = requireString(args, "name", description);
		if (!name.matches("[a-z0-9_-]{1,80}")) {
			throw new IllegalArgumentException("Invalid screenshot name on " + description);
		}
		return name;
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, String name) {
		return new TickRegressionActionExecution(context) {
			private int frames;
			private CompletableFuture<Void> future;

			@Override
			public RegressionActionProgress tick() {
				if (++frames < 5) {
					return RegressionActionProgress.WAITING;
				}
				if (future == null) {
					future = new CompletableFuture<>();
					Path destination = context.minecraft().gameDirectory.toPath().resolve("screenshots/regression").resolve(name + ".png");
					Screenshot.takeScreenshot(context.minecraft().gameRenderer.mainRenderTarget(), image -> {
						try (NativeImage captured = image) {
							Files.createDirectories(destination.getParent());
							captured.writeToFile(destination);
							context.putDiagnostic("screenshot", destination.toAbsolutePath().toString());
							future.complete(null);
						} catch (IOException exception) {
							future.completeExceptionally(new IllegalStateException("Unable to capture rendered frame", exception));
						}
					});
				}
				return await(future);
			}
		};
	}
}
