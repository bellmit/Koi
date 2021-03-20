package co.casterlabs.koi.config;

import java.util.List;

import lombok.Getter;

@Getter
public class BadgeConfiguration {
    private boolean ignoreExisting = false;
    private List<String> badges;

}
