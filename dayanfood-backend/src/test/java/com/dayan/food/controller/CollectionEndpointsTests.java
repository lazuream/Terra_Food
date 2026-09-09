package com.dayan.food.controller;

import com.dayan.food.entity.dto.EtchingDesignDTO;
import com.dayan.food.entity.vo.FavoriteStatusVO;
import com.dayan.food.service.EtchingDesignService;
import com.dayan.food.service.FavoriteService;
import com.dayan.food.service.WishlistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.ArrayList;
import java.util.Collections;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class CollectionEndpointsTests {
    @Test void favoriteRoutesBindTheDishAndSessionUser() throws Exception {
        var favorites = mock(FavoriteService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new CollectionController(favorites, mock(WishlistService.class))).build();
        var principal = new UsernamePasswordAuthenticationToken("tester", null);
        when(favorites.add(123L, "tester")).thenReturn(new FavoriteStatusVO(true));
        when(favorites.remove(123L, "tester")).thenReturn(new FavoriteStatusVO(false));
        mvc.perform(post("/api/profile/favorites/123").principal(principal))
                .andExpect(status().isOk()).andExpect(jsonPath("$.favorited").value(true));
        mvc.perform(delete("/api/profile/favorites/123").principal(principal))
                .andExpect(status().isOk()).andExpect(jsonPath("$.favorited").value(false));
        verify(favorites).add(123L, "tester");
        verify(favorites).remove(123L, "tester");
    }

    @Test void nullCellIsAValidationErrorNotAServerCrash() throws Exception {
        var service = mock(EtchingDesignService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new EtchingDesignController(service))
                .setControllerAdvice(new ApiExceptionHandler()).build();
        var colors = new ArrayList<>(Collections.nCopies(169, ""));
        colors.set(0, null);
        var payload = new ObjectMapper().writeValueAsString(new EtchingDesignDTO("Test", colors));
        mvc.perform(post("/api/etchings").principal(new UsernamePasswordAuthenticationToken("tester", null))
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
        verifyNoInteractions(service);
    }
}
