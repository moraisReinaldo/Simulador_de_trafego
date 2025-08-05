package com.simuladortrafego;

import java.util.concurrent.atomic.AtomicInteger;

public class TrafficLight extends Thread {
    private String id;
    private LightState currentState;
    private Intersection intersection; // The intersection this light belongs to
    private Direction controlledDirection; // The direction of traffic this light controls

    // Default timings (can be made dynamic later)
    private long greenTime = 15000; // 15 seconds
    private long yellowTime = 3000;  // 3 seconds
    private long redTime; // Calculated based on other lights in the intersection

    private volatile boolean running = true;
    private volatile boolean emergencyOverride = false;
    private AtomicInteger carsPassedOnYellow = new AtomicInteger(0);

    public TrafficLight(String id, Intersection intersection, Direction controlledDirection) {
        this.id = id;
        this.intersection = intersection;
        this.controlledDirection = controlledDirection;
        this.currentState = LightState.RED; // Default to RED
    }

    public LightState getCurrentState() {
        return currentState;
    }

    public String getLightId() {
        return id;
    }

    public Direction getControlledDirection() {
        return controlledDirection;
    }

    public long getGreenTime() { // Added to fix compilation error
        return greenTime;
    }

    public long getYellowTime() { // Added to fix compilation error
        return yellowTime;
    }

    public void setTimings(long greenTime, long yellowTime) {
        this.greenTime = greenTime;
        this.yellowTime = yellowTime;
        // Red time might be influenced by the intersection's cycle
    }

    // Chamado pelo Cruzamento para sincronizar
    public synchronized void turnGreen() {
        currentState = LightState.GREEN;
        System.out.println("Semáforo " + id + " em " + intersection.getId() + " para " + controlledDirection + " está VERDE");
        notifyAll(); // Notifica carros esperando neste semáforo
    }

    public synchronized void turnYellow() {
        currentState = LightState.YELLOW;
        carsPassedOnYellow.set(0); // Reseta o contador ao ficar amarelo
        System.out.println("Semáforo " + id + " em " + intersection.getId() + " para " + controlledDirection + " está AMARELO");
    }

    public synchronized void turnRed() {
        currentState = LightState.RED;
        System.out.println("Semáforo " + id + " em " + intersection.getId() + " para " + controlledDirection + " está VERMELHO");
    }

    public void activateEmergencyMode() {
        this.emergencyOverride = true;
        // Lógica para ficar verde imediatamente ou conforme protocolo de emergência
        // Isso pode envolver interromper o ciclo atual
        System.out.println("Semáforo " + id + " SUBSTITUIÇÃO DE EMERGÊNCIA ATIVADA");
        // Por enquanto, apenas uma flag, comportamento real a ser detalhado no Cruzamento ou controlador central
    }

    public void deactivateEmergencyMode() {
        this.emergencyOverride = false;
        System.out.println("Semáforo " + id + " SUBSTITUIÇÃO DE EMERGÊNCIA DESATIVADA");
    }

    public void carPassedOnYellow() {
        if (currentState == LightState.YELLOW) {
            carsPassedOnYellow.incrementAndGet();
        }
    }

    public int getCarsPassedOnYellow() {
        return carsPassedOnYellow.get();
    }

    @Override
    public void run() {
        // A lógica principal do ciclo (verde -> amarelo -> vermelho) será gerenciada pelo controlador do Cruzamento
        // Esta thread pode ser usada para atualizações de estado interno ou comportamentos individuais mais complexos do semáforo, se necessário.
        // Por enquanto, suas mudanças de estado são direcionadas pelo Cruzamento.
        System.out.println("Thread do Semáforo " + id + " iniciada.");
        while (running) {
            try {
                // Ajustes inteligentes baseados no tráfego poderiam ser implementados aqui
                // Por exemplo, consultando filas de carros do cruzamento
                // Isto é um placeholder para lógica individual mais avançada do semáforo
                if (!emergencyOverride) {
                    // Lógica de operação normal, se houver, independente do controle do cruzamento
                }
                Thread.sleep(1000); // Intervalo de verificação genérico
            } catch (InterruptedException e) {
                running = false;
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("Thread do Semáforo " + id + " parada.");
    }

    public void stopLight() {
        running = false;
        this.interrupt();
    }

    @Override
    public String toString() {
        return "TrafficLight{" +
                "id='" + id + '\'' +
                ", currentState=" + currentState +
                ", controlledDirection=" + controlledDirection +
                '}';
    }
}

