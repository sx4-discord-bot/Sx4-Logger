package com.sx4.bot;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Statistics {
	
	public static AtomicInteger failedAuditLogs = new AtomicInteger();
	public static AtomicInteger successfulAuditLogs = new AtomicInteger();
	
	private static final String MESSAGE = "Successful audit logs %s/%s (Unsuccessful: %s, %.2f%%)";
	
	static {
		new Thread(() -> {
			while(true) {
				int successful = successfulAuditLogs.get();
				int failed = failedAuditLogs.get();
				
				int total = successful + failed;
				
				System.out.println(String.format(MESSAGE, successful, total, failed, ((double) failed/total) * 100));
				
				try {
					Thread.sleep(TimeUnit.MINUTES.toMillis(5));
				}catch(InterruptedException e) {}
			}
		}).start();
	}
}