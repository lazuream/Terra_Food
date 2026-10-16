package com.dayan.food.service.impl;

import com.dayan.food.config.RegistrationVerificationSettings;
import com.dayan.food.mapper.AppUserMapper;
import com.dayan.food.service.AtomicChallengeStore;
import com.dayan.food.service.CaptchaService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocalRegistrationTests {
    @Test void cannotDisableVerificationOutsideLocalOrAlongsideProduction() {
        assertThrows(IllegalStateException.class, () -> new RegistrationVerificationSettings(false, new MockEnvironment()));
        var prod = new MockEnvironment(); prod.setActiveProfiles("local", "prod");
        assertThrows(IllegalStateException.class, () -> new RegistrationVerificationSettings(false, prod));
        assertTrue(new RegistrationVerificationSettings(true, prod).enabled());
    }

    @Test void localRegistrationSkipsEmailChallengesButNormalModeStillRequiresThem() {
        var env = new MockEnvironment(); env.setActiveProfiles("local");
        var mail = mock(JavaMailSender.class);
        var store = mock(AtomicChallengeStore.class);
        var service = service(mail, store, new RegistrationVerificationSettings(false, env));
        assertEquals("user@example.com", service.verify(" USER@example.com ", null));
        assertThrows(IllegalArgumentException.class, () -> service.sendCode("user@example.com", "unused", "0"));
        verifyNoInteractions(mail, store);
        var normal = service(mail, store, new RegistrationVerificationSettings(true, env));
        assertThrows(IllegalArgumentException.class, () -> normal.verify("user@example.com", null));
        assertThrows(IllegalArgumentException.class, () -> normal.verify("user@example.com", ""));
        normal.verify("user@example.com", "123456");
        verify(store).claim("registration", "user@example.com", "123456", 5, true);
    }

    private RegistrationCodeServiceImpl service(JavaMailSender mail, AtomicChallengeStore store,
            RegistrationVerificationSettings settings) {
        return new RegistrationCodeServiceImpl(mail, mock(StringRedisTemplate.class), mock(AppUserMapper.class),
                store, mock(CaptchaService.class), "", Duration.ofMinutes(10), Duration.ofSeconds(60), 5, settings);
    }
}
