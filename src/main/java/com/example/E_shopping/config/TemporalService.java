package com.example.E_shopping.config;

import com.example.E_shopping.Temporal.OrderWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class TemporalService {

    private WorkflowServiceStubs service;
    private WorkflowClient client;

    @PostConstruct
    public void startWorker() {
        // connect to temporla server locally
        WorkflowServiceStubsOptions options = WorkflowServiceStubsOptions.newBuilder()
                .setTarget("127.0.0.1:7233")
                .build();
        service = WorkflowServiceStubs.newInstance(options);

        // this is for workflow client
        client = WorkflowClient.newInstance(service);


        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker("DeliveryTaskQueue");
        worker.registerWorkflowImplementationTypes(OrderWorkflowImpl.class);
        factory.start();

        System.out.println("Temporal started on DeliveryTaskQueue");
    }

    // third party so we need to configure it as a bean
    @Bean
    public WorkflowClient workflowClient() {
        return client;
    }

    public WorkflowServiceStubs getService() {
        return service;
    }
}
