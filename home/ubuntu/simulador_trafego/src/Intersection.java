package com.simuladortrafego;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Intersection {
    private String id;
    private Map<Direction, TrafficLight> trafficLights; // Lights controlling entry FROM this direction
    private Map<Direction, List<Street>> incomingStreets;
    private Map<Direction, List<Street>> outgoingStreets;
    private Grid grid; // Reference to the grid for context

    // Basic cycle control for semaphores at this intersection
    private List<Direction> greenPhases; // Order of directions that get green light
    private int currentPhaseIndex = 0;
    private long phaseTime = 20000; // Default time for a full phase (e.g., N-S green, then E-W green)
    private long lastPhaseChangeTime;
    private final Lock phaseLock = new ReentrantLock();

    public Intersection(String id, Grid grid) {
        this.id = id;
        this.grid = grid;
        this.trafficLights = new HashMap<>();
        this.incomingStreets = new HashMap<>();
        this.outgoingStreets = new HashMap<>();
        this.greenPhases = new ArrayList<>();
        // Example: Default phases for a 4-way intersection
        // This needs to be configured based on actual connected streets
    }

    public String getId() {
        return id;
    }

    public void addIncomingStreet(Street street, Direction arrivalDirection) {
        this.incomingStreets.computeIfAbsent(arrivalDirection, k -> new ArrayList<>()).add(street);
        // Automatically create a traffic light for this incoming direction if it doesn't exist
        if (!trafficLights.containsKey(arrivalDirection)) {
            TrafficLight light = new TrafficLight("TL-" + id + "-" + arrivalDirection.name(), this, arrivalDirection);
            trafficLights.put(arrivalDirection, light);
            light.start(); // Start the traffic light thread
        }
    }

    public void addOutgoingStreet(Street street, Direction departureDirection) {
        this.outgoingStreets.computeIfAbsent(departureDirection, k -> new ArrayList<>()).add(street);
    }

    public TrafficLight getTrafficLight(Direction forDirection) {
        return trafficLights.get(forDirection);
    }

    public List<Street> getOutgoingStreets(Direction fromDirection) {
        // This logic needs to be more robust: cars arriving from 'fromDirection'
        // will want to go to other directions.
        // For now, let's assume it returns all streets not in 'fromDirection'
        List<Street> possibleExits = new ArrayList<>();
        for (Map.Entry<Direction, List<Street>> entry : outgoingStreets.entrySet()) {
            if (entry.getKey() != fromDirection) { // Simplified: cannot make a U-turn immediately
                possibleExits.addAll(entry.getValue());
            }
        }
        return possibleExits;
    }

    // Method to define the sequence of green lights
    public void setGreenPhases(List<Direction> phases) {
        this.greenPhases.clear();
        this.greenPhases.addAll(phases);
        this.currentPhaseIndex = 0;
        if (!phases.isEmpty()) {
            initializePhaseTimes();
        }
    }

    private void initializePhaseTimes() {
        // Set initial light states based on the first phase
        if (greenPhases.isEmpty()) return;
        Direction currentGreenDirection = greenPhases.get(currentPhaseIndex);
        for (Map.Entry<Direction, TrafficLight> entry : trafficLights.entrySet()) {
            if (entry.getKey() == currentGreenDirection || areCompatible(entry.getKey(), currentGreenDirection)) {
                entry.getValue().turnGreen();
            } else {
                entry.getValue().turnRed();
            }
        }
        lastPhaseChangeTime = System.currentTimeMillis();
    }

    // Basic check for compatible directions (e.g., North and South can be green together)
    private boolean areCompatible(Direction d1, Direction d2) {
        if (d1 == d2) return true;
        if ((d1 == Direction.NORTH && d2 == Direction.SOUTH) || (d1 == Direction.SOUTH && d2 == Direction.NORTH)) return true;
        if ((d1 == Direction.EAST && d2 == Direction.WEST) || (d1 == Direction.WEST && d2 == Direction.EAST)) return true;
        return false;
    }

    // This method would be called periodically by a simulator thread or its own thread
    public void updateSemaphores() {
        phaseLock.lock();
        try {
            if (greenPhases.isEmpty() || trafficLights.isEmpty()) {
                return;
            }

            long currentTime = System.currentTimeMillis();
            TrafficLight currentGreenLight = trafficLights.get(greenPhases.get(currentPhaseIndex));

            if (currentGreenLight == null) { // Não deve acontecer se configurado corretamente
                System.err.println("Erro: Nenhum semáforo para a fase verde atual: " + greenPhases.get(currentPhaseIndex) + " no cruzamento " + id);
                // Tenta recuperar ou avançar a fase
                advancePhase();
                return;
            }

            // Transition: Green -> Yellow
            if (currentGreenLight.getCurrentState() == LightState.GREEN && (currentTime - lastPhaseChangeTime) >= currentGreenLight.getGreenTime()) {
                currentGreenLight.turnYellow();
                // Also turn compatible lights yellow if they were green
                for (Direction dir : greenPhases) { // Check all directions in the current phase
                    if (areCompatible(dir, greenPhases.get(currentPhaseIndex)) && dir != greenPhases.get(currentPhaseIndex)) {
                        TrafficLight compatibleLight = trafficLights.get(dir);
                        if (compatibleLight != null && compatibleLight.getCurrentState() == LightState.GREEN) {
                            compatibleLight.turnYellow();
                        }
                    }
                }
                lastPhaseChangeTime = currentTime; // Reset timer for yellow phase
            }
            // Transition: Yellow -> Red, then advance phase
            else if (currentGreenLight.getCurrentState() == LightState.YELLOW && (currentTime - lastPhaseChangeTime) >= currentGreenLight.getYellowTime()) {
                currentGreenLight.turnRed();
                 // Also turn compatible lights red
                for (Direction dir : greenPhases) {
                    if (areCompatible(dir, greenPhases.get(currentPhaseIndex)) && dir != greenPhases.get(currentPhaseIndex)) {
                        TrafficLight compatibleLight = trafficLights.get(dir);
                        if (compatibleLight != null && compatibleLight.getCurrentState() == LightState.YELLOW) {
                            compatibleLight.turnRed();
                        }
                    }
                }
                advancePhase();
            }
            // Intelligent adjustments can be added here
            // e.g., check carsPassedOnYellow from currentGreenLight and adjust greenTime for next cycle
            checkCongestionAndAdjust(currentGreenLight);

        } finally {
            phaseLock.unlock();
        }
    }

    private void advancePhase() {
        currentPhaseIndex = (currentPhaseIndex + 1) % greenPhases.size();
        Direction nextGreenDirection = greenPhases.get(currentPhaseIndex);
        
        // Turn all other lights red first (important for safety)
        for (Map.Entry<Direction, TrafficLight> entry : trafficLights.entrySet()) {
            if (!areCompatible(entry.getKey(), nextGreenDirection)) {
                 if(entry.getValue().getCurrentState() != LightState.RED) entry.getValue().turnRed();
            }
        }
        
        // Now turn the new phase green
        TrafficLight nextGreenLight = trafficLights.get(nextGreenDirection);
        if (nextGreenLight != null) {
            nextGreenLight.turnGreen();
        } else {
             System.err.println("Erro: Nenhum semáforo para a próxima fase verde: " + nextGreenDirection + " no cruzamento " + id);
        }
        // Turn compatible lights green
        for (Direction dir : greenPhases) {
            if (areCompatible(dir, nextGreenDirection) && dir != nextGreenDirection) {
                TrafficLight compatibleLight = trafficLights.get(dir);
                if (compatibleLight != null) {
                    compatibleLight.turnGreen();
                }
            }
        }
        lastPhaseChangeTime = System.currentTimeMillis();
        System.out.println("Cruzamento " + id + " avançou para fase: " + nextGreenDirection);
    }

    private void checkCongestionAndAdjust(TrafficLight previousGreenLight) {
        if (previousGreenLight.getCarsPassedOnYellow() > 2) { // Limiar arbitrário
            System.out.println("Congestionamento detectado em " + id + " para a direção " + previousGreenLight.getControlledDirection() + ". Carros passaram no amarelo: " + previousGreenLight.getCarsPassedOnYellow());
            // Ajuste simples: aumenta ligeiramente o tempo de verde para esta fase na próxima vez
            long currentGreenTime = previousGreenLight.getGreenTime();
            previousGreenLight.setTimings(Math.min(currentGreenTime + 2000, 30000), previousGreenLight.getYellowTime()); // Aumenta em 2s, máx 30s
            System.out.println("Tempo de verde ajustado para " + previousGreenLight.getLightId() + " para " + previousGreenLight.getGreenTime() / 1000 + "s");
        }
    }

    public void handleEmergencyVehicle(Direction approachDirection) {
        System.out.println("Cruzamento " + id + " tratando veículo de emergência vindo de " + approachDirection);
        phaseLock.lock();
        try {
            for (Map.Entry<Direction, TrafficLight> entry : trafficLights.entrySet()) {
                TrafficLight light = entry.getValue();
                if (entry.getKey() == approachDirection) {
                    light.activateEmergencyMode(); // Sinaliza para o próprio semáforo
                    light.turnGreen(); // Força verde
                } else {
                    light.turnRed(); // Força vermelho para os outros
                }
            }
            // Potencialmente pausa o ciclo de fases normal ou tem um temporizador de fase de emergência especial
            lastPhaseChangeTime = System.currentTimeMillis(); // Reseta o temporizador da fase para dar tempo ao veículo de emergência
        } finally {
            phaseLock.unlock();
        }
    }

    public void endEmergencyMode(Direction approachDirection) {
        System.out.println("Cruzamento " + id + " finalizando modo de emergência para " + approachDirection);
        phaseLock.lock();
        try {
            TrafficLight light = trafficLights.get(approachDirection);
            if (light != null) {
                light.deactivateEmergencyMode();
            }
            // Retoma a operação normal - reinicializa as fases ou apenas deixa o ciclo continuar
            initializePhaseTimes(); // Ressincroniza os semáforos para a fase atual
        } finally {
            phaseLock.unlock();
        }
    }

    public void stopAllLights() {
        for (TrafficLight light : trafficLights.values()) {
            light.stopLight();
        }
    }

    @Override    public String toString() {
        return "Intersection{" +
                "id=\'" + id + "\\'}";
    }

    // Added based on Simulator.java comments
    public Map<Direction, TrafficLight> getTrafficLights() {
        return trafficLights;
    }

    public Map<Direction, List<Street>> getIncomingStreets() {
        return incomingStreets;
    }

    public Map<Direction, List<Street>> getOutgoingStreets() {
        return outgoingStreets;
    }

    public List<Street> getPossibleExits(Direction arrivalDirection) {
        List<Street> possible = new ArrayList<>();
        if (this.outgoingStreets == null) return possible;

        for (Map.Entry<Direction, List<Street>> entry : this.outgoingStreets.entrySet()) {
            // Basic rule: do not make an immediate U-turn if other options exist.
            // A U-turn would mean exiting in the opposite direction of arrival.
            // This logic can be more sophisticated, e.g. allowing U-turns if explicitly permitted or if no other exits.
            if (arrivalDirection == null || !isOpposite(arrivalDirection, entry.getKey())) {
                possible.addAll(entry.getValue());
            }
        }

        // If only a U-turn is possible (or no arrival direction specified, so all are considered),
        // and 'possible' is empty, then add all outgoing streets.
        if (possible.isEmpty() && !this.outgoingStreets.isEmpty()) {
            if (arrivalDirection != null && this.outgoingStreets.containsKey(getOppositeDirection(arrivalDirection))){
                 possible.addAll(this.outgoingStreets.get(getOppositeDirection(arrivalDirection)));
            } else { // If no specific opposite or no arrival direction, add all as last resort
                this.outgoingStreets.values().forEach(possible::addAll);
            }
        }
        return possible;
    }

    private boolean isOpposite(Direction d1, Direction d2) {
        if (d1 == null || d2 == null) return false;
        if (d1 == Direction.NORTH && d2 == Direction.SOUTH) return true;
        if (d1 == Direction.SOUTH && d2 == Direction.NORTH) return true;
        if (d1 == Direction.EAST && d2 == Direction.WEST) return true;
        if (d1 == Direction.WEST && d2 == Direction.EAST) return true;
        return false;
    }

    private Direction getOppositeDirection(Direction d) {
        if (d == null) return null;
        switch (d) {
            case NORTH: return Direction.SOUTH;
            case SOUTH: return Direction.NORTH;
            case EAST: return Direction.WEST;
            case WEST: return Direction.EAST;
            default: return null;
        }
    }
}

