package com.example.E_shopping.config;

import com.example.E_shopping.Temporal.OrderActivitiesImpl;
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
        // connect to temporal server locally
        WorkflowServiceStubsOptions options = WorkflowServiceStubsOptions.newBuilder()
                .setTarget("127.0.0.1:7233")
                .build();
        service = WorkflowServiceStubs.newInstance(options);

        // this is for workflow client
        client = WorkflowClient.newInstance(service);

        // ⚡ Make worker listen on the same queue as your workflow starts
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker("E_SHOPPING_TASK_QUEUE"); // use same queue
        worker.registerWorkflowImplementationTypes(OrderWorkflowImpl.class); // only OrderWorkflow
        worker.registerActivitiesImplementations(new OrderActivitiesImpl()); // register activities
        factory.start();

        System.out.println("Temporal started on E_SHOPPING_TASK_QUEUE");
    }

    @Bean
    public WorkflowClient workflowClient() {
        return client;
    }

    public WorkflowServiceStubs getService() {
        return service;
    }
}
