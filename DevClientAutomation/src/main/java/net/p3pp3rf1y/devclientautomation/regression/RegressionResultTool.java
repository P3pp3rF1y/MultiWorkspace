package net.p3pp3rf1y.devclientautomation.regression;

import com.google.gson.Gson;

import javax.annotation.Nullable;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/** Offline serialization of controller results using the runtime's result factory. */
public final class RegressionResultTool {
	private RegressionResultTool() {
	}

	public static void main(String[] args) {
		Gson gson = new Gson();
		Request request = gson.fromJson(new InputStreamReader(System.in, StandardCharsets.UTF_8), Request.class);
		System.out.println(gson.toJson(create(request)));
	}

	static RegressionResultModel.RunStatus create(Request request) {
		return RegressionResultModel.controller(request.kind(), request.suiteId(), request.testId(), request.message(), request.failure(),
				request.effectiveRequirements(), request.context());
	}

	record Request(RegressionResultModel.ControllerOutcome kind, String suiteId, String testId, String message, @Nullable RegressionResultModel.Failure failure,
			Set<String> effectiveRequirements, RegressionResultModel.ReportContext context) {
	}
}
