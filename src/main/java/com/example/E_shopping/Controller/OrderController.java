package com.example.E_shopping.Controller;

import com.example.E_shopping.Dto.OrderResponseDTO;
import com.example.E_shopping.Service.OrderService;
import com.example.E_shopping.Temporal.OrderWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private WorkflowClient workflowClient;

    // creation of oreder
    @PostMapping("/create")
    public ResponseEntity<OrderResponseDTO> createOrder(@RequestHeader("X-Auth") String token) {
        return ResponseEntity.ok(orderService.createOrder(token));
    }
    // pay for order
    @PostMapping("/pay/{orderId}")
    public ResponseEntity<OrderResponseDTO> payOrder(@RequestHeader("X-Auth") String token,
                                                     @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.payOrder(token, orderId));
    }
    // cancel order
    @PostMapping("/cancel/{orderId}")
    public ResponseEntity<String> cancelOrder(@RequestHeader("X-Auth") String token,
                                              @PathVariable Long orderId) {
        orderService.cancelOrder(token, orderId);
        return ResponseEntity.ok("Order cancelled successfully");
    }
    // return any order
    @PostMapping("/return/{orderId}")
    public ResponseEntity<String> returnOrder(@RequestHeader("X-Auth") String token,
                                              @PathVariable Long orderId) {
        orderService.returnOrder(token, orderId);
        return ResponseEntity.ok("Order returned successfully");
    }
    // to see order users
    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> getUserOrders(@RequestHeader("X-Auth") String token) {
        return ResponseEntity.ok(orderService.getUserOrders(token));
    }
    // payment using temporal
    @PostMapping("/checkout")
    public String checkout(@RequestParam String orderId, @RequestParam Double totalPrice, @RequestParam String userId) {
        OrderWorkflow workflow = workflowClient.newWorkflowStub(
                OrderWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue("E_SHOPPING_TASK_QUEUE")
                        .build()
        );

        WorkflowClient.start(() -> workflow.processOrder(orderId, totalPrice, userId));
        return "Order is being processed. You will be notified once payment and delivery are done.";
    }
}

