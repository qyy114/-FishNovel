package com.fishnovel.idea.service;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;

final class RemoteRedirectPolicy {
    private RemoteRedirectPolicy() {
    }

    static boolean isUnexpectedRedirect(URI requestUri, URI finalUri) {
        if (requestUri == null || finalUri == null) {
            return false;
        }
        String requestHost = requestUri.getHost();
        String finalHost = finalUri.getHost();
        if (requestHost == null || finalHost == null) {
            return false;
        }
        return !isSameHostFamily(requestHost, finalHost);
    }

    private static boolean isSameHostFamily(String requestHost, String finalHost) {
        String normalizedRequestHost = requestHost.toLowerCase(Locale.ROOT);
        String normalizedFinalHost = finalHost.toLowerCase(Locale.ROOT);
        return normalizedRequestHost.equals(normalizedFinalHost)
            || normalizedFinalHost.endsWith("." + normalizedRequestHost)
            || normalizedRequestHost.endsWith("." + normalizedFinalHost)
            || registrableDomain(normalizedRequestHost).equals(registrableDomain(normalizedFinalHost));
    }

    private static String registrableDomain(String host) {
        String[] labels = host.split("\\.");
        if (labels.length < 2) {
            return host;
        }
        int suffixLength = 2;
        String last = labels[labels.length - 1];
        String secondLast = labels[labels.length - 2];
        if (labels.length >= 3 && last.length() == 2 && secondLast.length() <= 3) {
            suffixLength = 3;
        }
        return String.join(".", Arrays.copyOfRange(labels, labels.length - suffixLength, labels.length));
    }
}
