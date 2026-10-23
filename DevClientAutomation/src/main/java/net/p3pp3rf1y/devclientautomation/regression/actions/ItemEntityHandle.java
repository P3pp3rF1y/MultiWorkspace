package net.p3pp3rf1y.devclientautomation.regression.actions;

import java.util.UUID;

/** Immutable identity, never a retained live entity or a nearby-entity query. */
public record ItemEntityHandle(UUID uuid, String dimension, String entityType, String item, int initialCount) {
	public void validate(String actualDimension, String actualType, String actualItem, int count) {
		if (!dimension.equals(actualDimension) || !entityType.equals(actualType) || !item.equals(actualItem) || count != initialCount) {
			throw new IllegalStateException("Item entity " + uuid + " expected " + dimension + "/" + entityType + "/" + item + " x" + initialCount
					+ ", observed " + actualDimension + "/" + actualType + "/" + actualItem + " x" + count);
		}
	}
}
