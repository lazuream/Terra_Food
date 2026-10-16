package com.dayan.food.security;
import com.dayan.food.config.ActiveSessionFilter;
import com.dayan.food.entity.enums.UserRole;
import com.dayan.food.entity.po.AppUser;
import com.dayan.food.entity.vo.AccountStatusVO;
import com.dayan.food.exception.UserServiceUnavailableException;
import com.dayan.food.mapper.AppUserMapper;
import com.dayan.food.service.AccountStatusReader;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class UserServiceSessionTests extends ActorTestSupport {
    private AppUser login() {
        var user=new AppUser("tester","encoded","Tester",UserRole.USER);
        actor(mock(AppUserMapper.class),"tester",user); return user;
    }
    @Test void outageRejectsProtectedRequestWithoutDestroyingSession() throws Exception {
        login();
        var request=new MockHttpServletRequest("GET","/api/profile/foods");
        var session=request.getSession(true); var response=new MockHttpServletResponse(); var chain=new MockFilterChain();
        new ActiveSessionFilter(id -> { throw new UserServiceUnavailableException(new RuntimeException()); }).doFilter(request,response,chain);
        assertEquals(503,response.getStatus()); assertNull(chain.getRequest());
        assertSame(session,request.getSession(false)); assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }
    @Test void revokedOrRecreatedAccountsCannotReuseRedisSessions() throws Exception {
        var user=login();
        AccountStatusVO[] states={null,new AccountStatusVO(user.getSubjectId(),1,false,UserRole.USER),
                new AccountStatusVO("other-subject",1,true,UserRole.USER),new AccountStatusVO(user.getSubjectId(),2,true,UserRole.USER)};
        for(var state:states) {
            login(); var request=new MockHttpServletRequest("GET","/api/profile/foods");
            var session=(MockHttpSession)request.getSession(true); var response=new MockHttpServletResponse(); var chain=new MockFilterChain();
            new ActiveSessionFilter(id -> state).doFilter(request,response,chain);
            assertEquals(401,response.getStatus()); assertTrue(session.isInvalid()); assertNull(chain.getRequest());
        }
    }
    @Test void anonymousCatalogReadDoesNotDependOnUserService() throws Exception {
        var accounts=mock(AccountStatusReader.class); var chain=new MockFilterChain();
        new ActiveSessionFilter(accounts).doFilter(new MockHttpServletRequest("GET","/api/foods/catalog"),new MockHttpServletResponse(),chain);
        verifyNoInteractions(accounts); assertNotNull(chain.getRequest());
    }
}
