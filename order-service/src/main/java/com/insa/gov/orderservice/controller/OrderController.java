package com.insa.gov.orderservice.controller;

import com.insa.gov.orderservice.dto.OrderLineItemsDto;
import com.insa.gov.orderservice.dto.OrderRequest;
import com.insa.gov.orderservice.dto.OrderResponseUpdate;
import com.insa.gov.orderservice.model.Order;
import com.insa.gov.orderservice.model.OrderLineItems;
import com.insa.gov.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String placeOrder(@RequestBody OrderRequest orderRequest) {
        orderService.placeOrder(orderRequest);
        return "Place Order is Successful";
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<OrderResponseUpdate> getOrderList(@RequestParam List<String> orderNumber) {
        return orderService.getOrderList(orderNumber);

    }
    @PutMapping("/updates/{id}")
    @ResponseStatus(HttpStatus.OK)
    public String updateParentAndChildren(@PathVariable Long id, @RequestBody OrderRequest orderRequest){
        orderService.updateParentAndChildren(id,orderRequest);
        return "update order list";
    }







//    @PostMapping("/new")
//    @ResponseStatus(HttpStatus.CREATED)
//    public Order createOrder(@RequestBody OrderRequest orderRequest) {
//        return orderService.createOrder(orderRequest.getOrder());
//
//    }



    @PutMapping("/updated/{id}")
    public void updateOrderInformationList(@PathVariable Long id,
                                                      @RequestBody List<OrderLineItemsDto> orderLineItemsDtoList){
         orderService.updateParentAndChildrenList(id,orderLineItemsDtoList);

    }
    @PutMapping("/updateList/{id}")
    @ResponseStatus(HttpStatus.OK)
    public OrderLineItems updateOrder(@PathVariable Long id,
                                      @RequestBody OrderLineItemsDto orderRequestDto){

        return orderService.updateOrderLineItems(id,orderRequestDto);

    }


}
