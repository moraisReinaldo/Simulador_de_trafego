package com.simuladortrafego;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Arrays;
import java.util.Map; // Added import for Map.Entry

public class Simulator {
    private Grid grid;
    private List<Car> cars;
    private List<Intersection> intersections;
    private volatile boolean running = true;
    private long simulationTime = 0;
    private final long tickDuration = 100; // milliseconds per simulation tick
    private final int maxCars = 10; // Max cars in simulation
    private Random random = new Random();

    public Simulator() {
        this.grid = new Grid();
        this.cars = new ArrayList<>();
        this.intersections = new ArrayList<>();
    }

    private void setupGrid() {
        // Create Intersections
        Intersection i1 = new Intersection("I1", grid);
        Intersection i2 = new Intersection("I2", grid);
        intersections.add(i1);
        intersections.add(i2);
        grid.addIntersection(i1);
        grid.addIntersection(i2);

        // Create Streets
        // S1: I1 (East) <-> I2 (West) - Two-way
        Street s1 = new Street("S1-I1E-I2W", 100, 10, true, null);
        // S2: North -> I1 (South) - One-way street entering I1 from North
        Street s2 = new Street("S2-N-I1S", 80, 5, false, Direction.SOUTH); // Cars travel SOUTH on this street to reach I1
        // S3: I1 (North) -> Exit - One-way street exiting I1 to North
        Street s3 = new Street("S3-I1N-Exit", 80, 5, false, Direction.NORTH); // Cars travel NORTH on this street from I1
        // S4: East -> I2 (West) - One-way street (as per user: "um deles tendo uma via de mão única")
        // This is the one-way street connected to one of the intersections.
        // Let's make S4 a one-way street leading TO I2 from East.
        Street s4 = new Street("S4-E-I2W", 70, 5, false, Direction.WEST); // Cars travel WEST on this street to reach I2
        // S5: I2 (East) -> Exit - One-way street exiting I2 to East (complementing S4)
        Street s5 = new Street("S5-I2E-Exit", 70, 5, false, Direction.EAST); // Cars travel EAST on this street from I2

        grid.addStreet(s1);
        grid.addStreet(s2);
        grid.addStreet(s3);
        grid.addStreet(s4);
        grid.addStreet(s5);

        // Connect streets to intersections
        // Intersection I1
        // S1 connects to I1 from West (cars on S1 going East arrive at I1 from West)
        // S1 connects to I1 from East (cars on S1 going West, this is the other direction of S1)
        grid.connectStreetToIntersection(s1.getId(), i1.getId(), Direction.WEST, true); // Arriving at I1 from West via S1
        grid.connectStreetToIntersection(s1.getId(), i1.getId(), Direction.EAST, false); // Departing I1 to East via S1

        grid.connectStreetToIntersection(s2.getId(), i1.getId(), Direction.NORTH, true); // Arriving at I1 from North via S2
        grid.connectStreetToIntersection(s3.getId(), i1.getId(), Direction.NORTH, false); // Departing I1 to North via S3

        // Intersection I2
        // S1 connects to I2 from East (cars on S1 going West arrive at I2 from East)
        grid.connectStreetToIntersection(s1.getId(), i2.getId(), Direction.EAST, true); // Arriving at I2 from East via S1
        grid.connectStreetToIntersection(s1.getId(), i2.getId(), Direction.WEST, false); // Departing I2 to West via S1

        // S4 is one-way INTO I2 from East
        grid.connectStreetToIntersection(s4.getId(), i2.getId(), Direction.EAST, true); // Arriving at I2 from East via S4
        // S5 is one-way OUT FROM I2 to East
        grid.connectStreetToIntersection(s5.getId(), i2.getId(), Direction.EAST, false); // Departing I2 to East via S5

        // Define green phases for intersections
        // I1: N-S and S-N (from S2, to S3) and E-W (S1)
        // Phase 1 for I1: S2 (North approach) gets green. S3 (North exit) is an exit.
        // Phase 2 for I1: S1 (West approach) gets green. S1 (East exit) is an exit.
        i1.setGreenPhases(Arrays.asList(Direction.NORTH, Direction.WEST)); // Simplified: Northbound/Southbound traffic, then Eastbound/Westbound
        
        // I2: E-W (S1, S4) and W-E (S1, S5)
        // Fase 1 para I2: S1 (chegada Leste) & S4 (chegada Leste) ficam verdes.
        // Fase 2 para I2: S1 (chegada Oeste) fica verde.
        i2.setGreenPhases(Arrays.asList(Direction.EAST)); // Tráfego do Leste apenas, conforme conexões atuais do grid

        System.out.println("Configuração do grid concluída.");
        grid.printGrid();
    }

    private void addCar() {
        if (cars.size() < maxCars && !grid.getStreets().isEmpty()) {
            Street startStreet = grid.getStreets().get(random.nextInt(grid.getStreets().size()));
            // Ensure car starts on a street that allows outbound movement initially
            // This is a simplification; a better way is to pick entry points to the grid.
            // For now, any street will do, car logic will try to navigate.
            if (startStreet.isTwoWay() || (!startStreet.isTwoWay() && startStreet.getDirection() != null)) {
                Car car;
                if (random.nextInt(10) == 0) { // 10% chance of being an emergency vehicle
                    car = new EmergencyVehicle(grid, startStreet, 5 + random.nextDouble() * 5); // velocidade 5-10
                } else {
                    car = new Car(grid, startStreet, 5 + random.nextDouble() * 5);
                }
                cars.add(car);
                car.start();
                System.out.println("Adicionado " + car.getCarId() + " à simulação na rua " + startStreet.getId());
            }
        }
    }

    public void startSimulation() {
        System.out.println("Iniciando simulação...");
        setupGrid();

        // Inicia controladores de cruzamento (se tiverem suas próprias threads para gerenciamento de fase)
        // No design atual, Intersection.updateSemaphores() é chamado pelo loop do simulador

        long lastCarAddTime = System.currentTimeMillis();

        while (running) {
            long loopStartTime = System.currentTimeMillis();
            simulationTime++;

            // Adiciona novos carros periodicamente
            if (System.currentTimeMillis() - lastCarAddTime > 5000) { // Adiciona um carro a cada 5 segundos
                addCar();
                lastCarAddTime = System.currentTimeMillis();
            }

            // Atualiza todos os cruzamentos (lógica de semáforo)
            for (Intersection intersection : intersections) {
                intersection.updateSemaphores();
            }

            // Movimento do carro é tratado por suas próprias threads.
            // Podemos verificar periodicamente os estados dos carros ou remover carros finalizados/presos.
            cars.removeIf(car -> !car.isAlive() || !((Car)car).isRunningSim()); // isRunningSim() a ser adicionado a Car

            // Imprime status periodicamente
            if (simulationTime % 50 == 0) { // A cada 5 segundos (50 * 100ms)
                printStatus();
            }

            try {
                long loopExecutionTime = System.currentTimeMillis() - loopStartTime;
                if (loopExecutionTime < tickDuration) {
                    Thread.sleep(tickDuration - loopExecutionTime);
                }
            } catch (InterruptedException e) {
                running = false;
                System.out.println("Simulador interrompido.");
                Thread.currentThread().interrupt();
            }

            if (simulationTime > 10000 && cars.isEmpty()) { // Para se a simulação rodar por muito tempo e não houver carros
                 // running = false;
                 // System.out.println("Simulação encerrada devido à inatividade.");
            }
        }
        stopSimulation();
    }

    private void printStatus() {
        System.out.println("\n--- Tick da Simulação: " + simulationTime + " ---");
        System.out.println("Carros Ativos: " + cars.size());
        for (Car car : cars) {
            if (car.isAlive()) {
                String streetInfo = car.getCurrentStreet() != null ? car.getCurrentStreet().getId() : "No Cruzamento";
                String intInfo = car.getCurrentIntersection() != null ? car.getCurrentIntersection().getId() : "Na Rua";
                System.out.println("- " + car.getCarId() + ": Rua: " + streetInfo + " (" + String.format("%.0f%%", car.getPositionOnStreet()*100) + ")" + ", Cruzamento: " + intInfo + (car instanceof EmergencyVehicle ? " (EMERGÊNCIA)" : ""));
            }
        }
        for (Intersection i : intersections) {
            System.out.println("Cruzamento " + i.getId() + ":");
            for (Map.Entry<Direction, TrafficLight> entry : i.getTrafficLights().entrySet()) {
                System.out.println("  Semáforo " + entry.getValue().getLightId() + " (" + entry.getKey() + "): " + entry.getValue().getCurrentState() + " (Passou no Amarelo: " + entry.getValue().getCarsPassedOnYellow() + ")");
            }
        }
        System.out.println("----------------------------\n");
    }

    public void stopSimulation() {
        System.out.println("Parando simulação...");
        running = false;
        for (Car car : cars) {
            car.stopCar();
        }
        for (Intersection intersection : intersections) {
            intersection.stopAllLights();
        }
        // Aguarda as threads finalizarem
        for (Car car : cars) {
            try {
                car.join(1000);
            } catch (InterruptedException e) { /* ignora */ }
        }
        System.out.println("Simulação parada.");
    }

    public static void main(String[] args) {
        Simulator simulator = new Simulator();
        Thread simulatorThread = new Thread(simulator::startSimulation);
        simulatorThread.start();

        // Para a simulação após algum tempo (ex: 2 minutos)
        try {
            Thread.sleep(120000); // 2 minutos
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        simulator.stopSimulation();
        try {
            simulatorThread.join(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Saindo do main.");
    }
}

// Add to Car.java:
// private volatile boolean runningSim = true;
// public boolean isRunningSim() { return runningSim; }
// In stopCar(): this.runningSim = false;
// In run() loop: if (!running) this.runningSim = false;

// Add to Intersection.java
// public Map<Direction, TrafficLight> getTrafficLights() { return trafficLights; }
// public Map<Direction, List<Street>> getIncomingStreets() { return incomingStreets; }
// public Map<Direction, List<Street>> getOutgoingStreets() { return outgoingStreets; }
/*
public List<Street> getPossibleExits(Direction arrivalDirection) {
    List<Street> possible = new ArrayList<>();
    for (Map.Entry<Direction, List<Street>> entry : outgoingStreets.entrySet()) {
        // Basic: don't make a U-turn into the same direction category immediately
        // This needs to be smarter based on actual street connections and one-way rules
        if (arrivalDirection == null || !isOpposite(arrivalDirection, entry.getKey())) { 
            possible.addAll(entry.getValue());
        }
    }
    if (possible.isEmpty() && !outgoingStreets.isEmpty()) { // If only U-turn is possible
        // Or if arrivalDirection was null
        outgoingStreets.values().forEach(possible::addAll);
    }
    return possible;
}
private boolean isOpposite(Direction d1, Direction d2) {
    if (d1 == Direction.NORTH && d2 == Direction.SOUTH) return true;
    if (d1 == Direction.SOUTH && d2 == Direction.NORTH) return true;
    if (d1 == Direction.EAST && d2 == Direction.WEST) return true;
    if (d1 == Direction.WEST && d2 == Direction.EAST) return true;
    return false;
}
*/

// Add to Grid.java
// In Car.java, when a car moves to a new street:
// setPreviousStreet(oldStreet); // before updating currentStreet
// This needs to be integrated into Car#chooseNextStreetAndDepart

