package org.example.rideshare;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.rideshare.repository.RideRepository;
import org.example.rideshare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "jwt.secret=test-secret-that-is-at-least-32-characters")
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class RideFlowIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RideRepository rideRepository;

    @BeforeEach
    void cleanDatabase() {
        rideRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ---------- auth ----------

    @Test
    void registerThenLoginReturnsJwt() throws Exception {
        register("john", "password1", "ROLE_USER").andExpect(status().isOk());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("username", "john", "password", "password1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void duplicateUsernameIsRejected() throws Exception {
        register("john", "password1", "ROLE_USER").andExpect(status().isOk());
        register("john", "password2", "ROLE_USER")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    @Test
    void wrongPasswordIs401() throws Exception {
        register("john", "password1", "ROLE_USER");

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("username", "john", "password", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void unknownRoleIsRejected() throws Exception {
        register("mallory", "password1", "ROLE_ADMIN")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void shortPasswordIsRejected() throws Exception {
        register("john", "1234", "ROLE_USER").andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpointWithoutTokenIs401() throws Exception {
        mvc.perform(get("/api/v1/user/rides"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void tamperedTokenIs401() throws Exception {
        String token = registerAndLogin("john", "ROLE_USER");
        mvc.perform(get("/api/v1/user/rides").header("Authorization", "Bearer " + token + "x"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- ride flow ----------

    @Test
    void fullRideFlowRequestAcceptComplete() throws Exception {
        String user = registerAndLogin("john", "ROLE_USER");
        String driver = registerAndLogin("dave", "ROLE_DRIVER");

        String rideId = createRide(user, "MG Road", "Airport");

        mvc.perform(get("/api/v1/driver/rides/requests").header("Authorization", bearer(driver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(rideId))
                .andExpect(jsonPath("$[0].status").value("REQUESTED"));

        mvc.perform(post("/api/v1/driver/rides/{id}/accept", rideId).header("Authorization", bearer(driver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.driverId").isNotEmpty());

        mvc.perform(post("/api/v1/rides/{id}/complete", rideId).header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mvc.perform(get("/api/v1/user/rides").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    void acceptedRideNoLongerListedAsPending() throws Exception {
        String user = registerAndLogin("john", "ROLE_USER");
        String driver = registerAndLogin("dave", "ROLE_DRIVER");
        String rideId = createRide(user, "A", "B");

        mvc.perform(post("/api/v1/driver/rides/{id}/accept", rideId).header("Authorization", bearer(driver)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/driver/rides/{id}/accept", rideId).header("Authorization", bearer(driver)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/driver/rides/requests").header("Authorization", bearer(driver)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void cannotCompleteRideThatWasNotAccepted() throws Exception {
        String user = registerAndLogin("john", "ROLE_USER");
        String rideId = createRide(user, "A", "B");

        mvc.perform(post("/api/v1/rides/{id}/complete", rideId).header("Authorization", bearer(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ride must be ACCEPTED to complete"));
    }

    @Test
    void createRideRequiresPickupAndDrop() throws Exception {
        String user = registerAndLogin("john", "ROLE_USER");
        mvc.perform(post("/api/v1/rides").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("pickupLocation", "", "dropLocation", "B")))
                .andExpect(status().isBadRequest());
    }

    // ---------- authorization ----------

    @Test
    void passengerCannotUseDriverEndpoints() throws Exception {
        String user = registerAndLogin("john", "ROLE_USER");
        mvc.perform(get("/api/v1/driver/rides/requests").header("Authorization", bearer(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    void driverCannotCreateRide() throws Exception {
        String driver = registerAndLogin("dave", "ROLE_DRIVER");
        mvc.perform(post("/api/v1/rides").header("Authorization", bearer(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("pickupLocation", "A", "dropLocation", "B")))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyThePassengerOrDriverCanCompleteARide() throws Exception {
        String user = registerAndLogin("john", "ROLE_USER");
        String driver = registerAndLogin("dave", "ROLE_DRIVER");
        String stranger = registerAndLogin("eve", "ROLE_USER");
        String rideId = createRide(user, "A", "B");
        mvc.perform(post("/api/v1/driver/rides/{id}/accept", rideId).header("Authorization", bearer(driver)))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/rides/{id}/complete", rideId).header("Authorization", bearer(stranger)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/rides/{id}/complete", rideId).header("Authorization", bearer(driver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void unknownRideIs404() throws Exception {
        String driver = registerAndLogin("dave", "ROLE_DRIVER");
        mvc.perform(post("/api/v1/driver/rides/{id}/accept", "does-not-exist").header("Authorization", bearer(driver)))
                .andExpect(status().isNotFound());
    }

    // ---------- docs ----------

    @Test
    void swaggerDocsArePublic() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("RideShare API"));
        mvc.perform(get("/")).andExpect(status().is3xxRedirection());
    }

    // ---------- helpers ----------

    private ResultActions register(String username, String password, String role) throws Exception {
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json("username", username, "password", password, "role", role)));
    }

    private String registerAndLogin(String username, String role) throws Exception {
        register(username, "password1", role).andExpect(status().isOk());
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("username", username, "password", "password1")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    private String createRide(String token, String pickup, String drop) throws Exception {
        String body = mvc.perform(post("/api/v1/rides").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("pickupLocation", pickup, "dropLocation", drop)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andReturn().getResponse().getContentAsString();
        JsonNode ride = objectMapper.readTree(body);
        return ride.get("id").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private String json(String... keyValues) throws Exception {
        var map = new java.util.LinkedHashMap<String, String>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return objectMapper.writeValueAsString(map);
    }
}
