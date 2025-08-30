package com.assignment.orderservice.errorhandling.exceptions;

public class OrderProcessingException extends RuntimeException{

    public OrderProcessingException(String message){
        super(message);
    }

     public OrderProcessingException(String message,Throwable cause){
        super(message,cause);
     }

}
