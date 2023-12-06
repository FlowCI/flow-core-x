package com.flowci.core.test;

import com.flowci.core.common.domain.Mongoable;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.user.domain.User;
import com.flowci.common.helper.HashingHelper;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    @BeforeEach
    public void login() {
        Mockito.when(sessionManager.get()).thenReturn(defaultAdminUser);
        Mockito.when(sessionManager.getUserEmail()).thenReturn(defaultAdminUser.getEmail());
        Mockito.when(sessionManager.getCurrentAuditor()).thenReturn(Optional.of(defaultAdminUser.getEmail()));
    }

    protected void shouldHasCreatedAtAndCreatedBy(Mongoable obj) {
        assertNotNull(obj.getCreatedAt());
        assertNotNull(obj.getCreatedBy());
        assertEquals(sessionManager.getUserEmail(), obj.getCreatedBy());
    }

    protected void shouldHasUpdatedAtAndUpdatedBy(Mongoable obj) {
        assertNotNull(obj.getUpdatedAt());
        assertNotNull(obj.getUpdatedBy());
        assertEquals(sessionManager.getUserEmail(), obj.getUpdatedBy());
    }
}
