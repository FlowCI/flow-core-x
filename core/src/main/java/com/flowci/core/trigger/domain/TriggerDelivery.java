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
@Document(collection = "trigger_delivery")
public class TriggerDelivery {

    @Id
    private String id;

    @Indexed(unique = true)
    private String triggerId;

    private List<Item> deliveries;

    // helper field for Dao
    private long deliveriesTotal;

    @Getter
    @Setter
    public static class Item {

        public enum Status {
            Success,

            Failure
        }

        private Status status; // success, failure

        private String desc; // description or error

        private Date timestamp;
    }
}
