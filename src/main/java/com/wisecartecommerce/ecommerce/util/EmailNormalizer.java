package com.wisecartecommerce.ecommerce.util;

import java.util.Set;

/**
 * Normalizes email addresses so that provider-specific tricks (Gmail-style
 * "+tag" aliasing and dot-insensitivity) can't be used to make the same
 * inbox look like multiple distinct identities for abuse-prevention checks.
 *
 * IMPORTANT: only use the normalized form for comparison/lookup purposes
 * (usage caps, dedup). Always send actual emails to the address the person
 * typed — normalization is safe for delivery too, since mail providers that
 * support +tag/dot-insensitivity route the normalized address to the same
 * mailbox, but preserving the original in records keeps things auditable.
 */
public final class EmailNormalizer {

    private static final Set<String> DOT_AND_PLUS_INSENSITIVE_DOMAINS = Set.of(
            "gmail.com", "googlemail.com"
    );

    private EmailNormalizer() {
    }

    public static String normalize(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            return rawEmail;
        }

        String email = rawEmail.trim().toLowerCase();
        int atIndex = email.lastIndexOf('@');
        if (atIndex <= 0) {
            return email; // not a well-formed address; leave as-is, validation happens elsewhere
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex + 1);

        // Strip +tag suffix (applies broadly — most providers honor this)
        int plusIndex = localPart.indexOf('+');
        if (plusIndex >= 0) {
            localPart = localPart.substring(0, plusIndex);
        }

        // Strip dots only for providers known to treat them as insignificant
        if (DOT_AND_PLUS_INSENSITIVE_DOMAINS.contains(domain)) {
            localPart = localPart.replace(".", "");
        }

        return localPart + "@" + domain;
    }
}