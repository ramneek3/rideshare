package org.example.rideshare.controller;

import jakarta.validation.Valid;
import org.example.rideshare.dto.CreateRideRequest;
import org.example.rideshare.model.Ride;
import org.example.rideshare.service.RideService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class UserRideController {

    @Autowired
    private RideService rideService;

    // Passenger creates ride
    @PostMapping("/rides")
    public Ride createRide(@Valid @RequestBody CreateRideRequest request,
                           Authentication auth) {
        return rideService.createRide(request, auth);
    }

    // Passenger gets their rides
    @GetMapping("/user/rides")
    public List<Ride> getMyRides(Authentication auth) {
        return rideService.getUserRides(auth);
    }

    // Complete ride (user or driver)
    @PostMapping("/rides/{id}/complete")
    public Ride completeRide(@PathVariable String id, Authentication auth) {
        return rideService.completeRide(id, auth);
    }
}