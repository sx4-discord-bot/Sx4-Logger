package com.sx4.bot;

import java.io.File;
import java.io.FileInputStream;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.BlockingDeque;
import java.util.stream.Collectors;

import org.json.JSONObject;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.net.Connection;
import com.sx4.bot.handler.EventHandler;
import com.sx4.bot.handler.ExceptionHandler;
import com.sx4.bot.handler.GuildMessageCache;

import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDA.ShardInfo;
import net.dv8tion.jda.core.JDA.Status;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.utils.cache.CacheFlag;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Sx4Logger {
	
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
	
	public static DateTimeFormatter getTimeFormatter() {
		return TIME_FORMATTER;
	}
	
	private static OkHttpClient client = new OkHttpClient();
	
	private static EventHandler eventHandler;
	
	private static ShardManager shardManager;
	
	public static EventHandler getEventHandler() {
		return Sx4Logger.eventHandler;
	}
	
	public static ShardManager getShardManager() {
		return Sx4Logger.shardManager;
	}
	
	public static final String MESSAGE_SEPARATOR = "------------------------------";
	
	public static void main(String[] args) throws Exception {
		String token;
		try(FileInputStream stream = new FileInputStream(new File("./config/sx4.token"))) {
			token = new String(stream.readAllBytes());
		}
		
		Connection connection = RethinkDB.r
			.connection()
			.db("sx4")
			.connect();
		
		Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
			System.err.println("[Uncaught]");
			
			exception.printStackTrace();
		});
		
		Sx4Logger.eventHandler = new EventHandler(connection);
		
		int shardCount = Sx4Logger.getRecommendedShards(token);
		
		Sx4Logger.shardManager = new DefaultShardManagerBuilder()
			.setToken(token)
			.setShardsTotal(shardCount)
			.setDisabledCacheFlags(EnumSet.of(CacheFlag.EMOTE, CacheFlag.GAME))
			.addEventListeners(new ExceptionHandler())
			.addEventListeners(Sx4Logger.eventHandler)
			.addEventListeners(GuildMessageCache.INSTANCE)
			.addEventListeners(new ListenerAdapter() {
				public void onReady(ReadyEvent event) {
					ShardInfo info = event.getJDA().getShardInfo();
					
					System.out.println("Started shard " + (info.getShardId() + 1) + "/" + info.getShardTotal());
					
					event.getJDA().removeEventListener(this);
				}
			})
			.build();
		

		while(Sx4Logger.shardManager.getShardCache().stream()
				.filter(jda -> jda != null)
				.filter(jda -> jda.getStatus().equals(Status.CONNECTED))
				.count() != shardCount) {
			
			Thread.sleep(100L);
		}
		
		System.gc();
		
		System.out.println("Booted!");
		
		/* Used for debugging */
		try(Scanner scanner = new Scanner(System.in)) {
			String line;
			while((line = scanner.nextLine()) != null) {
				if(line.startsWith("help")) {
					System.out.println(Sx4Logger.getMessageSeperated(new StringBuilder()
						.append("\nqueued - sends information about the queued requests")
						.append("\nstats - sends the statistics")
						.append("\nclear - clears the console")));
					
					continue;
				}
				
				if(line.equalsIgnoreCase("queued")) {
					StringBuilder message = new StringBuilder();
					
					Map<Long, BlockingDeque<EventHandler.Request>> queue = Sx4Logger.getEventHandler().getQueue();
					
					List<Long> mostQueued = queue.keySet().stream()
						.sorted((key, key2) -> -Integer.compare(queue.get(key).size(), queue.get(key2).size()))
						.limit(10)
						.collect(Collectors.toList());
					
					for(long guildId : mostQueued) {
						int queued = queue.get(guildId).size();
						if(queued > 0) {
							Guild guild = Sx4Logger.getShardManager().getGuildById(guildId);
							if(guild != null) {
								message.append('\n').append(guild.getName() + " (" + guildId + ") - " + queued);
							}else{
								message.append('\n').append("Unknown guild (" + guildId + ") - " + queued);
							}
						}
					}
					
					message.append('\n').append("Total queued requests: " + Sx4Logger.getEventHandler().getTotalRequestsQueued());

					System.out.println(Sx4Logger.getMessageSeperated(message));
					
					continue;
				}
				
				if(line.equalsIgnoreCase("stats")) {
					Statistics.printStatistics();
					
					continue;
				}
				
				if(line.equalsIgnoreCase("clear")) {
				    System.out.print("\033[H\033[2J");
				    System.out.flush();
				    
				    continue;
				}
				
				System.out.println(Sx4Logger.getMessageSeperated("\nUnknown command"));
			}
		}
	}
	
	public static StringBuilder getMessageSeperated(CharSequence sequence) {
		StringBuilder builder = new StringBuilder();
		builder.append(MESSAGE_SEPARATOR);
		builder.append(sequence);
		builder.append('\n').append(MESSAGE_SEPARATOR);
		
		return builder;
	}
	
	public static int getRecommendedShards(String token) {
		Request request = new Request.Builder()
			.url("https://discordapp.com/api/gateway/bot")
			.header("Authorization", "Bot " + token)
			.header("Content-Type", "application/json")
			.build();

		try(Response response = Sx4Logger.client.newCall(request).execute()) {
			return new JSONObject(response.body().string()).getInt("shards");
		}catch(Exception e) {
			throw new IllegalStateException("Failed to get recommended amount of shards", e);
		}
	}
}