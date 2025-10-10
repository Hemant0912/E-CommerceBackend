package com.example.E_shopping.Temporal;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface OrderWorkflow {
    @WorkflowMethod
    void processOrder(String orderId, Double totalPrice, String userId);

    @SignalMethod
    void scheduleRefund(String orderId, String userId, int daysDelay);

}

// we can only use one workflowmethod annotation in a class because
// it represents one full workdlow so if we use 2 workflow then
// the starting point will not be decided and error wil be
// coming so that is the reason

