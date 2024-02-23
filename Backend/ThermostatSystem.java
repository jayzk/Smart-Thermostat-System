import java.util.Random;

public class ThermostatSystem {
    double currentTemp;
    final double MAX_TEMP = 32.0;
    final double MIN_TEMP = 16.0;
    boolean isChangingTemperature = false;

    public ThermostatSystem(double initial_temp) {
        currentTemp = initial_temp;
        startFluctuationThread();
    }

    private void startFluctuationThread() {
        Thread fluctuationThread = new Thread(() -> {
            Random r = new Random();
            while (true) {
                if (!isChangingTemperature) {
                    double randomChange = Math.round((r.nextDouble() * 2 - 1) * 10.0) / 10.0;
                    currentTemp += randomChange;
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        fluctuationThread.start();
    }


    public double getCurrentTemp() {
        return Math.round(currentTemp * 10.0) / 10.0;
    }

    public void changeTemp(double new_temperature) {
        isChangingTemperature = true;
        System.out.println("Temperature change begin");
        Thread changeTempThread = new Thread(() -> {
            if (currentTemp < new_temperature) {
                while (currentTemp < new_temperature) {
                    currentTemp += 0.1;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else if (currentTemp > new_temperature) {
                while (currentTemp > new_temperature) {
                    currentTemp -= 0.1;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            System.out.println("Temperature change complete");
            isChangingTemperature = false;
        });
        changeTempThread.start();
    }



    // public static void main(String[] args) {
    //     ThermostatSystem thermostat1 = new ThermostatSystem(22.0f);

    //     // Check temperature
    //     for (int i = 0; i < 10; i++) {
    //         System.out.println("Current temperature: " + thermostat1.getCurrentTemp()); 
    //         try {
    //             Thread.sleep(1000);
    //         } catch (InterruptedException e) {
    //             e.printStackTrace();
    //         }
    //     }

    //     // Change temperature
    //     thermostat1.changeTemp(25.0f);
    //     while(thermostat1.getCurrentTemp() < 25.0f) {
    //         System.out.println("Current temperature: " + thermostat1.getCurrentTemp()); 
    //         try {
    //             Thread.sleep(1000);
    //         } catch (InterruptedException e) {
    //             e.printStackTrace();
    //         }
    //     }
    // }
}