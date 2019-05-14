package com.sx4.bot;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Statistics {
	
	private static final String AUDIT_MESSAGE = "Successful audit logs %s/%s (Unsuccessful: %s, %.2f%%)";
	private static final String WEBHOOK_MESSAGE = "Total webhooks registered %s";
	private static final String LOGS_MESSAGE = "Successful logs %s/%s (Skipped: %s, %.2f%%) (Unsuccessful: %s)";
	private static final String QUEUED_LOGS_MESSAGE = "Total queued logs %s";
	
	private static final String MESSAGE_SEPERATOR = "------------------------------";
	
	private static final AtomicInteger failedAuditLogs = new AtomicInteger();
	private static final AtomicInteger successfulAuditLogs = new AtomicInteger();
	
	private static final AtomicInteger successfulLogs = new AtomicInteger();
	private static final AtomicInteger failedLogs = new AtomicInteger();
	private static final AtomicInteger skippedLogs = new AtomicInteger();
	
	public static void increaseSuccessfulAuditLogs() {
		Statistics.successfulAuditLogs.incrementAndGet();
	}
	
	public static void increaseFailedAuditLogs() {
		Statistics.failedAuditLogs.incrementAndGet();
	}
	
	public static void increaseSuccessfulLogs() {
		Statistics.successfulLogs.incrementAndGet();
	}
	
	public static void increaseFailedLogs() {
		Statistics.failedLogs.incrementAndGet();
	}
	
	public static void increaseSkippedLogs() {
		Statistics.skippedLogs.incrementAndGet();
	}
	
	public static void printStatistics() {
		StringBuilder message = new StringBuilder();
		message.append(MESSAGE_SEPERATOR);
		
		{
			int successful = Statistics.successfulAuditLogs.get();
			int failed = Statistics.failedAuditLogs.get();
			
			int total = successful + failed;
			
			message.append('\n').append(String.format(AUDIT_MESSAGE, successful, total, failed, total != 0 ? ((double) failed/total) * 100 : 0));
		}
		
		{
			int successful = Statistics.successfulLogs.get();
			int skipped = Statistics.skippedLogs.get();
			int failed = Statistics.failedLogs.get();
			
			int total = successful + skipped;
			
			message.append('\n').append(String.format(LOGS_MESSAGE, successful, total, skipped, ((double) skipped/total) * 100, failed));
		}
		
		message.append('\n').append(String.format(WEBHOOK_MESSAGE, Sx4Logger.getEventHandler().getRegisteredWebhooks().size()));
		message.append('\n').append(String.format(QUEUED_LOGS_MESSAGE, Sx4Logger.getEventHandler().getTotalRequestsQueued()));
		
		message.append('\n').append(MESSAGE_SEPERATOR);
		System.out.println(message);
	}
	
	static {
		new Thread(() -> {
			while(true) {
				Statistics.printStatistics();
				
				try {
					Thread.sleep(TimeUnit.MINUTES.toMillis(5));
				}catch(InterruptedException e) {}
			}
		}).start();
	}
}