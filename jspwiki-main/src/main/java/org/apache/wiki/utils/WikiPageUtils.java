/*
 * Copyright (C) 2024 denkbares GmbH. All rights reserved.
 */

package org.apache.wiki.utils;

import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.pages.PageManager;

/**
 * @author Antonia Heyder (denkbares GmbH)
 * @created 11.01.2024
 */
public class WikiPageUtils {
	private static final String invalidCharsRegex = "[\\\\/|;:<>?*+]";
	private static final String invalidChars = "\\/|;:<>?*+";

	public static void checkDuplicatePagesCaseSensitive(Engine engine, String pageName) throws ProviderException {

		for (Page existingPage : engine.getManager(PageManager.class).getAllPages()) {
			if (existingPage.getName().equalsIgnoreCase(pageName)) {
				throw new ProviderException("Page already exists (case insensitive): " + pageName);
			}
		}
	}

	public static void checkIllegalCharacters(String renameTo) throws WikiException {
		if (renameTo.matches(".*" + invalidCharsRegex + ".*")) {
			throw new WikiException("Page name contains prohibited characters (" + invalidChars + ").");
		}
	}
}
