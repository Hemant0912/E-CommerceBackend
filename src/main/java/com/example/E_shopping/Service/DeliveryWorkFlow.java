package com.example.E_shopping.Service;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface DeliveryWorkFlow {
    @WorkflowMethod
    void deliverOrder(Long orderId);
}
