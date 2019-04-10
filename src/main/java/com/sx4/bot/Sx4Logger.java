package com.sx4.bot;

import java.io.File;
import java.io.FileInputStream;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;

import javax.security.auth.login.LoginException;

import org.json.JSONObject;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.net.Connection;
import com.sx4.bot.handler.EventHandler;
import com.sx4.bot.handler.ExceptionHandler;
import com.sx4.bot.handler.GuildMessageCache;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
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
			
			exception.getStackTrace();
		});
		
		JDABuilder builder = new JDABuilder(AccountType.BOT)
			.setToken(token)
			.addEventListener(new ExceptionHandler())
			.addEventListener(new EventHandler(connection))
			.addEventListener(GuildMessageCache.INSTANCE)
			.setDisabledCacheFlags(EnumSet.of(CacheFlag.EMOTE, CacheFlag.GAME));
		
		int shardCount = Sx4Logger.getRecommendedShards(token);
		for(int i = 0; i < shardCount; i++) {
			try {
				builder.useSharding(i, shardCount).build().awaitReady();
				
				System.out.println("Started shard " + (i + 1) + "/" + shardCount + "!");
			}catch(LoginException | InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		System.gc();
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