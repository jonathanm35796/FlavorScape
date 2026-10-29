package io.ruin.api.utils;

import java.util.Map;

public class XenPost {


    private static final String URL = System.getProperty("lletya.forumIntegrationUrl", "");

    private static final String AUTH = System.getenv("LLETYA_FORUM_AUTH");

    public static String post(String file, Map<Object, Object> map) {
        if (URL.isEmpty() || AUTH == null) throw new IllegalStateException("Lletya forum integration is not configured");
        map.put("auth", AUTH);
        map.put("file", file);
        return PostWorker.postArray(URL, map);
    }

}
