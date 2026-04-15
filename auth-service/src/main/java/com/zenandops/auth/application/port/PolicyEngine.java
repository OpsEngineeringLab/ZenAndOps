package com.zenandops.auth.application.port;

import com.zenandops.auth.domain.entity.User;
import com.zenandops.auth.domain.valueobject.AbacPolicy;
import com.zenandops.auth.domain.valueobject.RbacPolicy;

/**
 * Outbound port for evaluating authorization policies (RBAC and ABAC).
 */
public interface PolicyEngine {

    boolean evaluateRbac(User user, RbacPolicy policy);

    boolean evaluateAbac(User user, AbacPolicy policy);
}
