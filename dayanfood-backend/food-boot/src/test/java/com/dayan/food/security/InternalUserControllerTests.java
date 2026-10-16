package com.dayan.food.security;
import com.dayan.food.controller.InternalUserController;
import com.dayan.food.service.AccountStatusReader;
import com.dayan.food.service.PublicUserProfileReader;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class InternalUserControllerTests {
    @Test void invalidServiceTokensCannotReadAccountStateOrPublicMetadata() {
        var accounts=mock(AccountStatusReader.class); var profiles=mock(PublicUserProfileReader.class);
        var controller=new InternalUserController(accounts,profiles,"valid-internal-token-0123456789012345");
        for(String token:new String[]{null,"","forged-internal-token-0123456789012345"}) {
            assertEquals(401,assertThrows(ResponseStatusException.class,()->controller.identity(1L,token)).getStatusCode().value());
            assertEquals(401,assertThrows(ResponseStatusException.class,()->controller.profile(1L,token)).getStatusCode().value());
        }
        verifyNoInteractions(accounts,profiles);
    }
}
