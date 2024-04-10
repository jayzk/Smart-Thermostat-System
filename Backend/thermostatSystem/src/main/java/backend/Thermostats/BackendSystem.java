package backend.Thermostats;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Spring boot application to create the thermostat systems
 */
@Component
public class BackendSystem {
    @Value("${kafka.number-of-rooms}")
    private int numberOfRooms;

    @Bean
    public void createThermostatSystems() {

        //for each topic we have 2 queues
        //3 topics room1, 2 and 3
        //Queue partition id = 0: current temp
        //Queue partition id = 1: change temp

        for (int roomNum = 1; roomNum <= numberOfRooms; roomNum++) {
            createThermostatSystem("room" + roomNum);
        }
    }

    private void createThermostatSystem(String roomId) {
        new ThermostatSystem(roomId);
    }


}
