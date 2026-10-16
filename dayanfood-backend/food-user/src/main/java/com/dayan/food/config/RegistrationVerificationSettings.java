package com.dayan.food.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Registration-only development switch; password resets always require verification. */
@Component
public class RegistrationVerificationSettings {
    private final boolean enabled;

    public RegistrationVerificationSettings(
            @Value("${app.registration-code.verification-enabled:true}") boolean enabled,
            Environment environment) {
        if (!enabled && (!environment.matchesProfiles("local") || environment.matchesProfiles("prod"))) {
            throw new IllegalStateException("Registration email verification can only be disabled in the local profile");
        }
        this.enabled = enabled;
    }

    public boolean enabled() { return enabled; }
}
