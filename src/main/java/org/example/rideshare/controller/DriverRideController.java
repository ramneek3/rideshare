package org.example.rideshare.controller;

import org.example.rideshare.model.Ride;
import org.example.rideshare.service.RideService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/driver")
public class DriverRideController {

    @Autowired
    private RideService rideService;

    // View all pending requests
    @GetMapping("/rides/requests")
    public List<Ride> getPendingRides() {
        return rideService.getPendingRides();
    }

    // Accept ride
    @PostMapping("/rides/{id}/accept")
    public Ride acceptRide(@PathVariable String id, Authentication auth) {
        return rideService.acceptRide(id, auth);
    }
}