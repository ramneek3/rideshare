package org.example.rideshare.service;

import org.example.rideshare.dto.CreateRideRequest;
import org.example.rideshare.exception.BadRequestException;
import org.example.rideshare.exception.ForbiddenException;
import org.example.rideshare.exception.NotFoundException;
import org.example.rideshare.model.Ride;
import org.example.rideshare.model.User;
import org.example.rideshare.repository.RideRepository;
import org.example.rideshare.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class RideService {

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private UserRepository userRepository;

    public Ride createRide(@NonNull CreateRideRequest request, Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new BadRequestException("Unauthenticated user");
        }
        String username = auth.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!"ROLE_USER".equals(user.getRole())) {
            throw new ForbiddenException("Only passengers can create rides");
        }

        Ride ride = new Ride();
        ride.setUserId(user.getId());
        ride.setPickupLocation(request.getPickupLocation());
        ride.setDropLocation(request.getDropLocation());
        ride.setStatus("REQUESTED");
        ride.setCreatedAt(new Date());

        return rideRepository.save(ride);
    }

    public List<Ride> getUserRides(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new BadRequestException("Unauthenticated user");
        }
        String username = auth.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return rideRepository.findByUserId(user.getId());
    }

    public List<Ride> getPendingRides() {
        return rideRepository.findByStatus("REQUESTED");
    }

    public Ride acceptRide(@NonNull String rideId, Authentication auth) {
        if (rideId == null || rideId.isEmpty()) {
            throw new BadRequestException("Ride id must not be null or empty");
        }

        if (auth == null || auth.getName() == null) {
            throw new BadRequestException("Unauthenticated user");
        }
        String username = auth.getName();

        User driver = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!"ROLE_DRIVER".equals(driver.getRole())) {
            throw new ForbiddenException("Only drivers can accept rides");
        }

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new NotFoundException("Ride not found"));

        if (!"REQUESTED".equals(ride.getStatus())) {
            throw new BadRequestException("Ride is not in REQUESTED state");
        }

        ride.setDriverId(driver.getId());
        ride.setStatus("ACCEPTED");

        return rideRepository.save(ride);
    }

    public Ride completeRide(@NonNull String rideId, Authentication auth) {
        if (rideId == null || rideId.isEmpty()) {
            throw new BadRequestException("Ride id must not be null or empty");
        }

        if (auth == null || auth.getName() == null) {
            throw new BadRequestException("Unauthenticated user");
        }

        User caller = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new NotFoundException("Ride not found"));

        if (!caller.getId().equals(ride.getUserId()) && !caller.getId().equals(ride.getDriverId())) {
            throw new ForbiddenException("Only the ride's passenger or driver can complete it");
        }

        if (!"ACCEPTED".equals(ride.getStatus())) {
            throw new BadRequestException("Ride must be ACCEPTED to complete");
        }

        ride.setStatus("COMPLETED");
        return rideRepository.save(ride);
    }
}