package com.example.thermostatSystem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class HotelSystem {
    @Bean
    public void createThermostatSystems() {
        String[] roomIds = {"room1", "room2", "room3"}; // Add more room IDs as needed

        //for each topic we have 2 queues
        //3 topics room1, 2 and 3
        //Queue key = 0: current temp
        //Queue key = 1: change temp

        for (String roomId : roomIds) {
            System.out.println("room:" + roomId);
            createThermostatSystem(roomId);
        }
    }

    private void createThermostatSystem(String roomId) {
        new ThermostatSystem(roomId);
    }


}
