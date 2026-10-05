package com.app.apigateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;
class ApiGatewayApplicationTests {

    @Test
    void mainMethodExistsAndIsStatic() throws Exception {
        Method main = ApiGatewayApplication.class.getMethod("main", String[].class);
        assertThat(Modifier.isStatic(main.getModifiers())).isTrue();
    }

    @Test
    void applicationClassIsLoadable() {
        assertThat(ApiGatewayApplication.class.getName()).isEqualTo("com.app.apigateway.ApiGatewayApplication");
    }

}
