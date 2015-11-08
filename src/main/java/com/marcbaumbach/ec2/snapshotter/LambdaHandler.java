package com.marcbaumbach.ec2.snapshotter;

import java.util.concurrent.TimeUnit;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.lambda.runtime.Context;

public class LambdaHandler {

	public String performBackup(Object scheduledEvent, Context context) {
		long start = System.nanoTime();
		new Snapshotter(new EnvironmentVariableCredentialsProvider()).createSnapshots().cleanupSnapshots();
		return String.format("Total execution time: %s(ms)", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
	}
	
}