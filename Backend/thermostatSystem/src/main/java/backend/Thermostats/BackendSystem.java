package backend.Thermostats;

import backend.Thermostats.ThermostatSystem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class BackendSystem {
    @Value("${kafka.number-of-rooms}")
    private int numberOfRooms;

    @Bean
    public void createThermostatSystems() {

        //for each topic we have 2 queues
        //3 topics room1, 2 and 3
        //Queue key = 0: current temp
        //Queue key = 1: change temp


        for (int roomNum = 1; roomNum <= numberOfRooms; roomNum++) {
            createThermostatSystem("room" + roomNum);
        }
    }

    private void createThermostatSystem(String roomId) {
        new ThermostatSystem(roomId);
    }


}
