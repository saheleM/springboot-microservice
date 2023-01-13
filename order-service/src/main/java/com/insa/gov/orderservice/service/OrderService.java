package com.insa.gov.orderservice.service;

import com.insa.gov.orderservice.dto.InventoryResponse;
import com.insa.gov.orderservice.dto.OrderLineItemsDto;
import com.insa.gov.orderservice.dto.OrderRequest;
import com.insa.gov.orderservice.dto.OrderResponseUpdate;
import com.insa.gov.orderservice.model.Order;
import com.insa.gov.orderservice.model.OrderLineItems;
import com.insa.gov.orderservice.repository.OrderLineItemRepository;
import com.insa.gov.orderservice.repository.OrderRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
//import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderLineItemRepository orderLineItemRepository;
    private final WebClient.Builder webClientBuilder;


    public void placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(getOrderNoWithYear());
        order.setOrderTable(orderRequest.getOrderTable());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDtoList)
                .toList();


        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode)
                .toList();

        InventoryResponse[] inventoryResponsesArray = webClientBuilder.build().get()
                .uri("http://inventory-service/api/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();

        boolean allProductsInStock = Arrays.stream(inventoryResponsesArray).allMatch(InventoryResponse::isInStock);
        if (allProductsInStock) {
            orderRepository.save(order);
        } else {
            throw new IllegalArgumentException("Product is not stock, Please order again!");
        }


    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());


        return orderLineItems;
    }

    private OrderLineItems mapToDtoList(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setId(orderLineItemsDto.getId());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());



        return orderLineItems;
    }


    public String getOrderNoWithYear() {

        String refNo; // Format Of Reference Number REF/001/07
        Integer seqNo = 0;

        DateFormat dateFormat = new SimpleDateFormat("yyyy");

        Date _date = new Date();
        String yourDate = dateFormat.format(_date);

        Order orderInfo = new Order();
        orderInfo = orderRepository.findOrderMax();
        String sequNo = null;

        if (orderInfo != null) {

            Integer seqRefNo = Integer.parseInt(orderInfo.getOrderNumber().split("/")[1]);
            String year = orderInfo.getOrderNumber().split("/")[2];
            if (year.equals(yourDate)) {
                seqNo = seqRefNo + 1;

                if (String.valueOf(seqNo).length() == 1) {
                    sequNo = "000000" + seqNo;
                } else if (String.valueOf(seqNo).length() == 2) {
                    sequNo = "00000" + seqNo;
                } else if (String.valueOf(seqNo).length() == 3) {
                    sequNo = "0000" + seqNo;
                } else if (String.valueOf(seqNo).length() == 4) {
                    sequNo = "000" + seqNo;
                } else if (String.valueOf(seqNo).length() == 5) {
                    sequNo = "00" + seqNo;
                } else if (String.valueOf(seqNo).length() == 6) {
                    sequNo = "0" + seqNo;
                }

                refNo = "OR_NO/" + sequNo + "/" + dateFormat.format(_date); // remain work get current year
            } else {
                seqNo = seqNo + 1;

                if (String.valueOf(seqNo).length() == 1) {
                    sequNo = "000000" + seqNo;
                }
                refNo = "OR_NO/" + sequNo + "/" + dateFormat.format(_date);
            }
        } else {
            seqNo = seqNo + 1;

            if (String.valueOf(seqNo).length() == 1) {
                sequNo = "000000" + seqNo;
            }
            refNo = "OR_NO/" + sequNo + "/" + dateFormat.format(_date);
        }
        return refNo;
    }


    public void updateParentAndChildrenList(Long id, List<OrderLineItemsDto> orderLineItemsDtoList) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Parent Not Found"));

        List<OrderLineItems> orderLineItemsList = orderLineItemRepository.findAllByOrderId(id);

        System.out.printf("====order list size ========="+order.getOrderLineItemsList().size());
        System.out.printf("====orderLineItems list size ========="+order.getOrderLineItemsList().size());
        orderLineItemsList.forEach(orderLineItems -> {
            orderLineItemsDtoList.forEach(c->{
                if(c.getId() == orderLineItems.getOrder().getId()){
                    orderLineItems.setQuantity(c.getQuantity());
                    orderLineItems.setPrice(c.getPrice());
                    orderLineItems.setSkuCode(c.getSkuCode());

                }
            });
        });
        orderRepository.saveAndFlush(order);
        orderLineItemRepository.saveAll(orderLineItemsList);
    }

    public void updatePlaceOrder(Long id, OrderRequest orderRequest) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Parent not found"));
        System.out.printf("=========order list size out of for loop======= "+order.getOrderLineItemsList().size()+" "+order.getOrderNumber());
        //order.setOrderNumber(getOrderNoWithYear());
        order.setOrderTable(orderRequest.getOrderTable());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDtoList)
                .toList();


        order.setOrderLineItemsList(orderLineItems);

//        List<String> skuCodes = order.getOrderLineItemsList().stream()
//                .map(OrderLineItems::getSkuCode)
//                .toList();
//
//        InventoryResponse[] inventoryResponsesArray = webClientBuilder.build().get()
//                .uri("http://inventory-service/api/inventory",
//                        uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
//                .retrieve()
//                .bodyToMono(InventoryResponse[].class)
//                .block();
//
//        boolean allProductsInStock = Arrays.stream(inventoryResponsesArray).allMatch(InventoryResponse::isInStock);
//        if (allProductsInStock) {
            orderRepository.save(order);
//        } else {
//            throw new IllegalArgumentException("Product is not stock, Please order again!");
//        }


    }

//    public OrderLineItems updateOrderLineItems(Long id, OrderLineItemsDto orderLineItemsDto) {
//
//        OrderLineItems orderLineItems=new OrderLineItems();
//        orderLineItems=  orderLineItemRepository.findById(id).get();
//        orderLineItems.setId(orderLineItemsDto.getId());
//        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
//        orderLineItems.setPrice(orderLineItemsDto.getPrice());
//        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
//
//
//
//        return orderLineItemRepository.save(orderLineItems);
//
//    }

    public OrderLineItems updateOrderLineItems(Long id, OrderLineItemsDto orderLineItemsDto) {

        return orderLineItemRepository.findById(id)
                .map(orderLineItems -> {
                    orderLineItems.setId(orderLineItemsDto.getId());
                    orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
                    orderLineItems.setPrice(orderLineItemsDto.getPrice());
                    orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
                    return orderLineItemRepository.save(orderLineItems);
                }).orElseThrow(() -> new RuntimeException("Order line Item Not Found"));


    }

    public Order createOrder(Order order) {
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseUpdate> getOrderList(List<String> orderNumber) {
        return orderRepository.findByOrderNumberIn(orderNumber).stream()
                .map(order ->
                        OrderResponseUpdate.builder()
                                .orderNumber(order.getOrderNumber())
                                .orderTable(order.getOrderTable())
                                .orderLineItemsList(order.getOrderLineItemsList())

                                .build()
                ).toList();
    }


    public void updateOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(getOrderNoWithYear());
        order.setOrderTable(orderRequest.getOrderTable());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();


        order.setOrderLineItemsList(orderLineItems);
    }

    public Order updateParentAndChildren(Long id, OrderRequest orderRequest) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Parent not found"));
        System.out.printf("=========order list size out of for loop======= "+order.getOrderLineItemsList().size()+" "+order.getOrderNumber());
//        List<OrderLineItems> updatedOrderList=new ArrayList<>();
//        updatedOrderList=order.getOrderLineItemsList();

        for (OrderLineItems orderItems: order.getOrderLineItemsList()) {
            System.out.printf("=========orderlist size ======= "+order.getOrderLineItemsList().size()+" "+order.getOrderNumber());
//            orderItems.setQuantity(89);
//            order.getOrderLineItemsList().add(orderItems);

            //OrderLineItems updatedOrderLines = new OrderLineItems();
            for(OrderLineItemsDto orderLineItemsDto: orderRequest.getOrderLineItemsDtoList()) {
                    if(orderLineItemsDto.getOrder_id()==orderItems.getId()){
                        order.setOrderTable(orderRequest.getOrderTable());
                        orderItems.setOrder(order);
                        orderItems.setPrice(orderLineItemsDto.getPrice());
                        orderItems.setQuantity(orderLineItemsDto.getQuantity());
                        orderItems.setSkuCode(orderLineItemsDto.getSkuCode());
//order.getOrderLineItemsList().add(orderItems);

                    }

                order.setOrderLineItemsList(orderItems.getOrder().getOrderLineItemsList());
//                // set the relationship
//                orderItems.setQuantity(orderLineItemsDto.getQuantity());
//                orderItems.setPrice(orderLineItemsDto.getPrice());
//                orderItems.setSkuCode(orderLineItemsDto.getSkuCode());
//
//                orderItems.setOrder(order);
//                order.getOrderLineItemsList().add(orderItems);
//                System.out.printf("=========orderlist size in for loop ======= "+" "+order.getOrderNumber());
//
            }
//            order.setOrderLineItemsList(orderItems.getOrder().getOrderLineItemsList());
            //order.setOrderLineItemsList(orderItems.getOrder().getOrderLineItemsList());
        }


        return orderRepository.saveAndFlush(order);


    }

}
