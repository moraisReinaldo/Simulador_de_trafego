package com.simuladortrafego;

public class EmergencyVehicle extends Car {
    private boolean sirenOn = false;

    public EmergencyVehicle(Grid grid, Street startStreet, double initialSpeed) {
        super(grid, startStreet, initialSpeed);
        this.sirenOn = true; // Veículos de emergência geralmente têm sirenes ligadas ao responder
        System.out.println("VeiculoDeEmergencia " + getCarId() + " criado e sirene LIGADA.");
    }

    public boolean isSirenOn() {
        return sirenOn;
    }

    public void toggleSiren() {
        this.sirenOn = !this.sirenOn;
        System.out.println("VeiculoDeEmergencia " + getCarId() + " sirene agora está " + (sirenOn ? "LIGADA" : "DESLIGADA"));
    }

    // Override move or arriveAtIntersection methods if emergency vehicles
    // have different behavior (e.g., can ignore red lights under certain conditions
    // or trigger emergency protocols at intersections).

    @Override
    protected void arriveAtIntersection(Intersection intersection) {
        System.out.println("VeiculoDeEmergencia " + getCarId() + " chegou em " + intersection.getId() + ". Solicitando prioridade.");
        setCurrentIntersection(intersection);
        setCurrentStreet(null); // Não está mais em um segmento de rua específico
        setPositionOnStreet(0);

        Direction arrivalDirection = getGrid().getArrivalDirection(this, intersection, getPreviousStreet());
        if (arrivalDirection == null) {
            System.err.println("Erro: VeiculoDeEmergencia " + getCarId() + " não conseguiu determinar a direção de chegada em " + intersection.getId());
            stopCar(); // Não pode prosseguir
            return;
        }

        if (isSirenOn()) {
            intersection.handleEmergencyVehicle(arrivalDirection);
        }

        // Veículos de emergência podem ter lógica diferente para prosseguir.
        // Por enquanto, assume-se que eles obtêm sinal verde ou podem prosseguir com cautela.
        // Um sistema mais complexo faria outros carros cederem passagem, etc.
        // TrafficLight lightToObey = intersection.getTrafficLight(arrivalDirection);

        // Simplificado: se a sirene estiver ligada, assume-se que o cruzamento dá prioridade.
        // Aguarda um período muito curto para simular a reação do cruzamento, depois prossegue.
        if (isSirenOn()) {
            try {
                System.out.println("VeiculoDeEmergencia " + getCarId() + " aguardando brevemente o cruzamento " + intersection.getId() + " liberar para " + arrivalDirection);
                Thread.sleep(500); // Simula o tempo de reação do cruzamento
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                stopCar();
                return;
            }
        } else {
            // Se a sirene estiver desligada, comporta-se como um carro normal (ou segue protocolo específico não emergencial)
            super.arriveAtIntersection(intersection); // Retorna ao comportamento normal do carro
            return; // super.arriveAtIntersection cuidará da partida
        }
        
        System.out.println("VeiculoDeEmergencia " + getCarId() + " avançando por " + intersection.getId() + " vindo de " + arrivalDirection + " com prioridade.");
        chooseNextStreetAndDepart(intersection);

        // Após passar, sinaliza o fim do modo de emergência para esta travessia específica do cruzamento
        if (isSirenOn()) {
            intersection.endEmergencyMode(arrivalDirection);
        }
    }

    // chooseNextStreetAndDepart is inherited from Car, can be overridden if needed.
}

