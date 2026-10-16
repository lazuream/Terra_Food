package com.dayan.food.controller;

import com.dayan.food.entity.vo.ProfileStatsVO;
import com.dayan.food.service.ProfileStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileStatsControllerTests {
    @Test
    void usesSessionIdentityEvenWhenAnotherUserIdIsSupplied() throws Exception {
        ProfileStatsService service = mock(ProfileStatsService.class);
        when(service.getMine("explorer")).thenReturn(new ProfileStatsVO(1234, 87));
        var mvc = MockMvcBuilders.standaloneSetup(new ProfileStatsController(service)).build();

        mvc.perform(get("/api/profile/stats")
                        .param("userId", "999")
                        .principal(new UsernamePasswordAuthenticationToken("explorer", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewedFoodCount").value(1234))
                .andExpect(jsonPath("$.favoriteCount").value(87));
        verify(service).getMine("explorer");
    }
}
