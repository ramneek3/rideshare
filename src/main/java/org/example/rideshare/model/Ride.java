package org.example.rideshare.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "rides")
@Data
public class Ride {

    @Id
    private String id;

    private String userId;     // Passenger
    private String driverId;   // Driver (nullable)

    private String pickupLocation;
    private String dropLocation;

    // REQUESTED / ACCEPTED / COMPLETED
    private String status;

    private Date createdAt;
}