package net.p3pp3rf1y.devclientautomation.regression.actions;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;

import java.util.LinkedHashMap;
import java.util.Map;

/** Run-scoped receipt observer for the exact jukebox sound expected from a held backpack. */
final class HeldBackpackJukeboxSoundObserver implements SoundEventListener {
	private final SoundManager soundManager;
	private final Identifier expectedSound;
	private int receiptCount;
	private int armedReceiptCount;
	private Map<String, Object> lastReceipt = Map.of();

	HeldBackpackJukeboxSoundObserver(SoundManager soundManager, Identifier expectedSound) {
		this.soundManager = soundManager;
		this.expectedSound = expectedSound;
		soundManager.addListener(this);
	}

	void arm() {
		armedReceiptCount = receiptCount;
	}

	boolean hasReceiptSinceArming() {
		return receiptCount > armedReceiptCount;
	}

	int receiptCount() {
		return receiptCount;
	}

	Map<String, Object> diagnostics() {
		Map<String, Object> diagnostics = new LinkedHashMap<>();
		diagnostics.put("expectedSound", expectedSound.toString());
		diagnostics.put("receiptCount", receiptCount);
		diagnostics.put("armedReceiptCount", armedReceiptCount);
		diagnostics.put("receiptSinceArming", hasReceiptSinceArming());
		diagnostics.put("lastReceipt", lastReceipt);
		return Map.copyOf(diagnostics);
	}

	void remove() {
		soundManager.removeListener(this);
		lastReceipt = Map.of();
	}

	@Override
	public void onPlaySound(SoundInstance sound, WeighedSoundEvents soundEvent, float range) {
		if (!sound.getIdentifier().equals(expectedSound) || sound.getSource() != SoundSource.RECORDS) {
			return;
		}
		receiptCount++;
		Map<String, Object> receipt = new LinkedHashMap<>();
		receipt.put("sound", sound.getIdentifier().toString());
		receipt.put("source", sound.getSource().getName());
		receipt.put("relative", sound.isRelative());
		receipt.put("x", sound.getX());
		receipt.put("y", sound.getY());
		receipt.put("z", sound.getZ());
		receipt.put("range", range);
		lastReceipt = Map.copyOf(receipt);
	}
}
