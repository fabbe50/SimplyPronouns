package com.fabbe50.simplypronouns;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.events.client.ClientChatEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Scanner;

public class SimplyPronouns {
	public static final String MOD_ID = "simplypronouns";
	private static final String LOOKUP_URL = "https://pronoundb.org/api/v2/lookup?platform=minecraft&ids=";
	private static final File registerFile = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath() + "/simply-pronouns.txt");
	private static final HashMap<String, String> pronounCache = new HashMap<>();

	public static void init() {
		ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> {
			if (player.is(Minecraft.getInstance().player)) {
				if (firstTimeUse()) {
					player.sendSystemMessage(Component.literal("Thank you for using Simply Pronouns!"));
					setUsed();
				}
				String pronoun = getCachedShortFormPronoun(player.getStringUUID(), false);
				if (pronoun.isEmpty()) {
					player.sendSystemMessage(
							Component.literal("You don't have your pronouns registered! Go to ")
									.append(Component.literal("https://pronoundb.org/login")
											.withStyle(Style.EMPTY.withColor(ChatFormatting.BLUE)
													.withUnderlined(true)
													.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://pronoundb.org/login"))
											)
									)
									.append(" and log in with your Minecraft account to set your pronouns.")
					);
				}
			}
		});

		ClientChatEvent.RECEIVED.register((type, message) -> {
			String username = type.name().getString();
			ClientPacketListener packetListener = Minecraft.getInstance().getConnection();
			if (packetListener != null) {
				for (PlayerInfo info : packetListener.getOnlinePlayers()) {
					GameProfile profile = info.getProfile();
					if (profile.getName().equals(username)) {
						String pronoun = getCachedShortFormPronoun(profile.getId().toString(), false);
						if (!pronoun.isEmpty()) {
							String sMessage = message.getString().substring(message.getString().indexOf(">") + 2);
							message = Component.literal("<").append(username).append(" ").append(Component.literal(pronoun).withStyle(ChatFormatting.GRAY)).append("> ").append(sMessage);
							return CompoundEventResult.interruptTrue(message);
						}
					}
				}
			}
			return CompoundEventResult.pass();
		});
	}

	public static String getCachedShortFormPronoun(String uuid, boolean threaded) {
		if (pronounCache.containsKey(uuid)) {
			return pronounCache.get(uuid);
		} else {
			if (threaded) {
				return threadedGetAndCacheShortFormPronoun(uuid);
			} else {
				return unthreadedGetAndCacheShortFormPronoun(uuid);
			}
		}
	}

	public static String unthreadedGetAndCacheShortFormPronoun(String uuid) {
		String shortFormPronoun = "";
		pronounCache.put(uuid, shortFormPronoun);
		try {
			URL url = new URL(LOOKUP_URL + uuid);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.connect();
			int responseCode = conn.getResponseCode();
			if (responseCode != 200) {
				System.err.print("HttpResponseCode: " + responseCode);
			} else {
				String pronoun = getPronounFromURL(url, uuid);
				shortFormPronoun = Pronouns.getShortFormFromID(pronoun);
				pronounCache.replace(uuid, shortFormPronoun);
			}
		} catch (IOException e) {
			System.err.print(e.getMessage());
		}
		return shortFormPronoun;
	}

	public static String threadedGetAndCacheShortFormPronoun(String uuid) {
		final String[] shortFormPronoun = new String[]{""};
		pronounCache.put(uuid, shortFormPronoun[0]);
		new Thread(() -> {
			try {
				URL url = new URL(LOOKUP_URL + uuid);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");
				conn.connect();
				int responseCode = conn.getResponseCode();
				if (responseCode != 200) {
					System.err.print("HttpResponseCode: " + responseCode);
				} else {
					String pronoun = getPronounFromURL(url, uuid);
					shortFormPronoun[0] = Pronouns.getShortFormFromID(pronoun);
					pronounCache.replace(uuid, shortFormPronoun[0]);
				}
			} catch (IOException e) {
				System.err.print(e.getMessage());
			}
		}).start();
		return shortFormPronoun[0];
	}

	private static String getPronounFromURL(URL url, String uuid) throws IOException {
		String pronoun = "";
		StringBuilder inline = new StringBuilder();
		Scanner scanner = new Scanner(url.openStream());
		while (scanner.hasNext()) {
			inline.append(scanner.nextLine());
		}
		scanner.close();
		JsonElement element = JsonParser.parseString(inline.toString());
		JsonObject object = element.getAsJsonObject();
		if (object.has(uuid)) {
			object = object.getAsJsonObject(uuid);
			if (object.has("sets")) {
				object = object.getAsJsonObject("sets");
				if (object.has("en")) {
					JsonArray jsonArray = object.getAsJsonArray("en");
					if (!jsonArray.isEmpty()) {
						if (jsonArray.size() >= 2) {
							pronoun = jsonArray.get(0).getAsString() + "/" + jsonArray.get(1).getAsString();
						} else {
							pronoun = jsonArray.get(0).getAsString();
						}
					}
				}
			}
		}
		return pronoun;
	}

	public enum Pronouns {
		//Nominative
		HE("he", "He/Him", "He/Him pronouns"),
		IT("it", "It/Its", "It/Its pronouns"),
		SHE("she", "She/Her", "She/Her pronouns"),
		THEY("they", "They/Them", "They/Them pronouns"),
		//Meta Sets
		ANY("any", "Any", "Any pronouns"),
		ASK("ask", "Ask", "Ask me my pronouns"),
		AVOID("avoid", "Avoid", "Avoid pronouns, use my name"),
		OTHER("other", "Other", "Other pronouns");

		final String id;
		final String shortForm;
		final String description;

		Pronouns(String id, String shortForm, String description) {
			this.id = id;
			this.shortForm = shortForm;
			this.description = description;
		}

		public static String getShortFormFromID(String id) {
			for (Pronouns pronoun : values()) {
				if (pronoun.id.equals(id)) {
					return pronoun.shortForm;
				}
			}
			return "";
		}
	}

	private static boolean firstTimeUse() {
		if (!registerFile.exists()) {
			writeTextFile(registerFile, "true");
			return true;
		} else return !readTextFile(registerFile).equals("false");
    }

	private static void setUsed() {
		writeTextFile(registerFile, "false");
	}

	private static void writeTextFile(File file, String s) {
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			FileOutputStream fos = new FileOutputStream(file, false);
			fos.write(s.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static String readTextFile(File file) {
		if (file.exists()) {
			try {
				FileInputStream fis = new FileInputStream(file);
				return new String(fis.readAllBytes(), StandardCharsets.UTF_8);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return "";
	}
}
