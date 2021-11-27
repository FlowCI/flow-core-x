package com.flowci.core.trigger.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@Document("trigger_delivery")
public class TriggerDelivery {

    @Id
    private String id;

    @Indexed(unique = true)
    private String triggerId;

    private List<Item> deliveries;

    @Getter
    @Setter
    public static class Item {

        private String status; // success, failure

        private String error;

        private Date timestamp;
    }
}
