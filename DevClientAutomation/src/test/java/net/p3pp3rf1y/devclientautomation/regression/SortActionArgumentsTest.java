package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SortActionArgumentsTest {
	@Test
	void bindRequiresAnExactSortControl() {
		for (String control : List.of("sort", "mode")) {
			assertDoesNotThrow(() -> bind("menu.clickSortControl", "{\"control\":\"" + control + "\"}"));
		}
		for (String args : List.of("{}", "{\"control\":\"other\"}", "{\"control\":1}", "{\"control\":\"sort\",\"extra\":true}")) {
			assertThrows(IllegalArgumentException.class, () -> bind("menu.clickSortControl", args));
		}
	}

	@Test
	void bindRejectsUnknownOrMalformedSortModes() {
		for (String mode : List.of("NAME", "MOD", "COUNT", "TAGS")) {
			assertDoesNotThrow(() -> bind("assert.sortMode", "{\"mode\":\"" + mode + "\"}"));
		}
		for (String args : List.of("{}", "{\"mode\":\"count\"}", "{\"mode\":false}", "{\"mode\":\"COUNT\",\"extra\":true}")) {
			assertThrows(IllegalArgumentException.class, () -> bind("assert.sortMode", args));
		}
	}

	private void bind(String action, String args) {
		new RegressionActionRegistry().require(action).bind(JsonParser.parseString(args).getAsJsonObject(), "sort action arguments");
	}
}
