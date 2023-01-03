package com.flowci.core.test;

import com.flowci.core.common.domain.Mongoable;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.user.domain.User;
import com.flowci.util.HashingHelper;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

public abstract class MockLoggedInScenario extends SpringScenario{

    private final static User defaultAdminUser = new User();

    static {
        defaultAdminUser.setId("112233");
        defaultAdminUser.setRole(User.Role.Admin);
        defaultAdminUser.setEmail("test@flow.ci");
        defaultAdminUser.setPasswordOnMd5(HashingHelper.md5("12345"));
    }

    @MockBean
    protected SessionManager sessionManager;

    @Before
    public void login() {
        Mockito.when(sessionManager.get()).thenReturn(defaultAdminUser);
        Mockito.when(sessionManager.getUserEmail()).thenReturn(defaultAdminUser.getEmail());
        Mockito.when(sessionManager.getCurrentAuditor()).thenReturn(Optional.of(defaultAdminUser.getEmail()));
    }

    protected void shouldHasCreatedAtAndCreatedBy(Mongoable obj) {
        Assert.assertNotNull(obj.getCreatedAt());
        Assert.assertNotNull(obj.getCreatedBy());
        Assert.assertEquals(sessionManager.getUserEmail(), obj.getCreatedBy());
    }

    protected void shouldHasUpdatedAtAndUpdatedBy(Mongoable obj) {
        Assert.assertNotNull(obj.getUpdatedAt());
        Assert.assertNotNull(obj.getUpdatedBy());
        Assert.assertEquals(sessionManager.getUserEmail(), obj.getUpdatedBy());
    }

}
