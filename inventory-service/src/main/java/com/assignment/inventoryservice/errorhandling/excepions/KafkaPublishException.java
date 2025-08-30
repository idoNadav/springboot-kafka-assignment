package com.assignment.inventoryservice.errorhandling.excepions;

public class KafkaPublishException extends RuntimeException{

    public KafkaPublishException(String message){
        super(message);
    }

    public KafkaPublishException(String message, Throwable cause){
        super(message,cause);
    }
}
