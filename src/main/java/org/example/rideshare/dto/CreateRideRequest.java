package org.example.rideshare.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateRideRequest {

    @NotBlank(message = "Pickup is required")
    private String pickupLocation;

    @NotBlank(message = "Drop is required")
    private String dropLocation;
}