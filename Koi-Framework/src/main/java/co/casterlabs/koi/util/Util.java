package co.casterlabs.koi.util;

import java.net.URLDecoder;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.SneakyThrows;

public class Util {

    public static Map<String, List<String>> splitQuery(String resourceDescriptor) {
        String[] split = resourceDescriptor.split("\\?", 2);

        if (split.length == 2) {
            String query = split[1];
            // @formatter:off
            return Arrays.stream(query.split("&"))
                    .map(Util::splitQueryParameter)
                    .collect(
                            Collectors.groupingBy(
                                    SimpleImmutableEntry::getKey,
                                    HashMap::new,
                                    Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                            )
                    );
            // @formatter:on
        } else {
            return Collections.emptyMap();
        }
    }

    @SneakyThrows
    private static SimpleImmutableEntry<String, String> splitQueryParameter(String it) {
        int idx = it.indexOf("=");
        String key = (idx > 0) ? it.substring(0, idx) : it;
        String value = ((idx > 0) && (it.length() > idx + 1)) ? it.substring(idx + 1) : "";

        return new SimpleImmutableEntry<>(URLDecoder.decode(key, "UTF-8"), URLDecoder.decode(value, "UTF-8"));
    }

}
