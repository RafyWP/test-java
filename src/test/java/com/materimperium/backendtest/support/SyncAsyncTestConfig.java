package com.materimperium.backendtest.support;

import java.util.concurrent.Executor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class SyncAsyncTestConfig {

    @Bean(name = "processamentoExecutor")
    public Executor processamentoExecutor() {
        return Runnable::run;
    }
}
