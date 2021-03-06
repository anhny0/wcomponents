package com.github.bordertech.wcomponents.util;

import com.github.bordertech.wcomponents.Message;
import com.github.bordertech.wcomponents.UIContext;
import com.github.bordertech.wcomponents.UIContextHolder;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Internationalisation utility methods.
 *
 * @author Yiannis Paschalidis
 * @since 1.0.0
 */
public final class I18nUtilities {

	/**
	 * The logger instance for this class.
	 */
	private static final Log LOG = LogFactory.getLog(I18nUtilities.class);

	/**
	 * A store of bad bundles, to avoid repeated logging of errors when the bundle fails to load.
	 */
	private static final Set<Locale> MISSING_RESOURCES = new HashSet<>();

	/**
	 * Prevent instantiation of this utility class.
	 */
	private I18nUtilities() {
	}

	/**
	 * Retrieves the resource bundle base name.
	 *
	 * @return the resource bundle base name.
	 */
	public static String getResourceBundleBaseName() {
		return ConfigurationProperties.getI18nResourceBundleBaseName();
	}

	/**
	 * Converts a message String and optional message arguments to a an appropriate format for internationalisation.
	 *
	 * @param text the message text.
	 * @param args the message arguments.
	 * @return a message in an appropriate format for internationalisation, may be null.
	 */
	public static Serializable asMessage(final String text, final Serializable... args) {
		if (text == null) {
			return null;
		} else if (args == null || args.length == 0) {
			return text;
		} else {
			return new Message(text, args);
		}
	}

	/**
	 * Attempts to format the given message for the given locale.
	 *
	 * <p>
	 * If a locale is provided, this method will attempt to load the text from a resource bundle. If
	 * <code>message</code> is a {@link Message} and has format arguments, then {@link MessageFormat} will be used to
	 * format the final message.</p>
	 *
	 * @param locale the locale to use, or null to use the current UIContext's Locale.
	 * @param message the message object to format.
	 * @return the formatted message.
	 */
	public static String format(final Locale locale, final Object message) {
		if (message instanceof Message) {
			String text = ((Message) message).getMessage();
			Serializable[] args = ((Message) message).getArgs();

			if (text == null) {
				return null;
			}

			return format(locale, text, (Object[]) args);
		} else if (message != null) {
			return format(locale, message.toString(), (Object[]) null);
		}

		return null;
	}

	/**
	 * Formats the given text, optionally using locale-specific text.
	 *
	 * @param locale The locale to use to look up a message, may be null.
	 * @param text the text / resource bundle key to use.
	 * @param args the message arguments, may be null.
	 * @return the formatted text.
	 */
	public static String format(final Locale locale, final String text, final Object... args) {
		if (text == null) {
			return null;
		}

		String localisedText = getLocalisedText(locale, text);
		String message = localisedText == null ? text : localisedText;

		if (args != null) {
			try {
				return MessageFormat.format(message, args);
			} catch (IllegalArgumentException e) {
				LOG.error("Invalid message format for message " + message, e);
			}
		}

		return message;
	}

	/**
	 * Get the locale currently in use.
	 * @return The currently active locale.
	 */
	public static Locale getEffectiveLocale() {
		Locale effectiveLocale = null;
		UIContext uic = UIContextHolder.getCurrent();

		if (uic != null) {
			effectiveLocale = uic.getLocale();
		}

		if (effectiveLocale == null) {
			String defaultLocale = ConfigurationProperties.getDefaultLocale();
			effectiveLocale = new Locale(defaultLocale);
		}

		// Don't use Locale.getDefault() because it is too nebulous (depends on host environment)

		return effectiveLocale;
	}

	/**
	 * Attempts to retrieve the localized message for the given text.
	 *
	 * @param locale the locale to retrieve the message for, or null for the default locale.
	 * @param text the text message or id to look up.
	 * @return the localised text if found, otherwise null.
	 */
	public static String getLocalisedText(final Locale locale, final String text) {
		String message = null;
		String resourceBundleBaseName = getResourceBundleBaseName();

		if (!Util.empty(resourceBundleBaseName)) {
			Locale effectiveLocale = locale;

			if (effectiveLocale == null) {
				effectiveLocale = getEffectiveLocale();
			}

			try {
				// TODO: This is slow
				ResourceBundle bundle = ResourceBundle.getBundle(resourceBundleBaseName, effectiveLocale);
				message = bundle.getString(text);
			} catch (MissingResourceException e) {
				// Fall back to the Configuration mechanism for the default internal messages
				if (text != null && text.startsWith(ConfigurationProperties.INTERNAL_MESSAGE_PREFIX)) {
					message = ConfigurationProperties.getInternalMessage(text);
				}

				if (message == null && !MISSING_RESOURCES.contains(locale)) {
					LOG.error("Missing resource mapping for locale: " + locale + ", text: " + text);
					MISSING_RESOURCES.add(locale);
				}
			}
		} else if (text != null && text.startsWith(ConfigurationProperties.INTERNAL_MESSAGE_PREFIX)) {
			// Fall back to the Configuration mechanism for the default internal messages
			message = ConfigurationProperties.getInternalMessage(text);
		}

		return message;
	}
}
