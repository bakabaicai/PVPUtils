package com.pvp_utils;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PVPUtils implements ModInitializer {
	public static final String MOD_ID = "pvp-utils";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		LOGGER.info("Welcome to use PVPUtils!");
	}
}