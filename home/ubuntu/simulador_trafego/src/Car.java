package com.simuladortrafego;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class Car extends Thread {
    private static final AtomicInteger idGenerator = new AtomicInteger(0);
    private String carId;
    protected Grid grid; // Changed to protected for EmergencyVehicle
    protected Street currentStreet; // Changed to protected
    private Street previousStreet;
    protected Intersection currentIntersection; // Changed to protected
    private Intersection nextIntersection;
    protected double positionOnStreet; // Changed to protected
    private double speed;
    private volatile boolean running = true;
    private volatile boolean runningSim = true;
    private Random random = new Random();
    private Direction lastArrivalDirection = null; // Added to store arrival direction at an intersection

    public Car(Grid grid, Street startStreet, double initialSpeed) {
        this.carId = "Carro-" + idGenerator.incrementAndGet();
        this.grid = grid;
        this.currentStreet = startStreet;
        this.previousStreet = null; // Inicializa previousStreet
        this.speed = initialSpeed;
        this.positionOnStreet = 0.0;
        // Determina a próximaIntersecao inicial com base na startStreet
        // Esta é uma suposição simplificada; um grid real forneceria isso
        List<Intersection> connectedIntersections = grid.getIntersectionsConnectedToStreet(startStreet);
        if (!connectedIntersections.isEmpty()) {
            // Para simplificar, escolhe uma. Um sistema real precisa saber a direção do carro na rua.
            // Isso precisa ser refinado com base em como Grid e Street definem as conexões.
            // Assumindo que o carro começa no "início" da rua e segue em direção a uma de suas extremidades.
            this.nextIntersection = connectedIntersections.get(random.nextInt(connectedIntersections.size()));
        } else {
            System.err.println("Aviso: Carro " + carId + " criado em uma rua sem cruzamentos conectados.");
            this.nextIntersection = null; // Ou tratar como um erro
        }
        System.out.println(carId + " criado na rua " + startStreet.getId() + " indo em direção a " + (nextIntersection != null ? nextIntersection.getId() : "N/A"));
    }

    public String getCarId() {
        return carId;
    }

    @Override
    public void run() {
        System.out.println(carId + " iniciando jornada.");
        while (running) {
            try {
                move();
                Thread.sleep(100); // Velocidade do tick de simulação para movimento do carro
            } catch (InterruptedException e) {
                running = false;
                System.out.println(carId + " interrompido e parando.");
                Thread.currentThread().interrupt();
            }
        }
        System.out.println(carId + " terminou a jornada.");
    }

    private void move() {
        if (currentStreet == null || nextIntersection == null) {
            // Carro está preso ou no final de um caminho sem mais conexões
            // System.out.println(carId + " não tem para onde ir.");
            // Por enquanto, vamos fazer com que ele saia da simulação ou apenas pare
            // running = false;
            // return;
            // Tenta encontrar uma nova rota se estiver preso, ou remove o carro
            if (currentIntersection != null) { // Preso em um cruzamento sem saídas válidas
                 System.out.println(carId + " está em " + currentIntersection.getId() + " e decidindo o próximo movimento.");
                 List<Street> possibleExits = grid.getExitingStreetsFromIntersection(currentIntersection, null); // null para qualquer direção
                 if (possibleExits.isEmpty()) {
                    System.out.println(carId + " não encontrou saídas de " + currentIntersection.getId() + ". Terminando jornada.");
                    running = false;
                    return;
                 }
                 currentStreet = possibleExits.get(random.nextInt(possibleExits.size()));
                 currentIntersection = null;
                 positionOnStreet = 0;
                 // Determina o próximo cruzamento novamente
                 List<Intersection> connectedIntersections = grid.getIntersectionsConnectedToStreet(currentStreet);
                 // Esta lógica precisa ser cuidadosa com a direção na nova rua
                 if (!connectedIntersections.isEmpty()) {
                    // Filtra o cruzamento de onde acabamos de vir, se possível
                    // Por enquanto, escolha aleatória simples
                    this.nextIntersection = connectedIntersections.get(random.nextInt(connectedIntersections.size()));
                    System.out.println(carId + " moveu para nova rua " + currentStreet.getId() + " em direção a " + this.nextIntersection.getId());
                 } else {
                    System.out.println(carId + " moveu para rua " + currentStreet.getId() + " que não tem mais cruzamentos. Terminando jornada.");
                    running = false;
                    return;
                 }
            } else {
                System.out.println(carId + " não tem rua atual ou próximo cruzamento. Terminando jornada.");
                running = false; 
                return;
            }
        }

        positionOnStreet += speed / currentStreet.getLength(); // Movimento simplificado

        if (positionOnStreet >= 1.0) { // Chegou ao final da rua (aproximando-se do nextIntersection)
            System.out.println(carId + " chegou ao final de " + currentStreet.getId() + ", aproximando-se de " + nextIntersection.getId());
            positionOnStreet = 1.0; // Limita no final
            arriveAtIntersection(nextIntersection);
        } else {
            // System.out.println(carId + " está em " + String.format("%.2f", positionOnStreet * 100) + "% na rua " + currentStreet.getId());
        }
    }

     protected void arriveAtIntersection(Intersection intersection) {
        System.out.println(carId + " chegou em " + intersection.getId() + ". Aguardando sinal verde.");
        this.currentIntersection = intersection;
        this.previousStreet = this.currentStreet; // Define previousStreet como a rua que acabou de atravessar
        this.currentStreet = null; // Não está mais em um segmento de rua específico, mas no nó do cruzamento
        this.positionOnStreet = 0;

        // Determina a direção de chegada no cruzamento usando a previousStreet agora corretamente definida
        this.lastArrivalDirection = grid.getArrivalDirection(this, intersection, this.previousStreet);
        if (this.lastArrivalDirection == null) {
            System.err.println("Erro: " + carId + " não conseguiu determinar a direção de chegada em " + intersection.getId() + " vindo da rua anterior " + (this.previousStreet != null ? this.previousStreet.getId() : "nula") );
            running = false; // Não pode prosseguir sem saber qual semáforo obedecer
            runningSim = false;
            return;
        }

        TrafficLight lightToObey = intersection.getTrafficLight(this.lastArrivalDirection);
        if (lightToObey == null) {
            System.err.println("Erro: " + carId + " não encontrou semáforo para a direção " + this.lastArrivalDirection + " em " + intersection.getId());
            chooseNextStreetAndDepart(intersection);
            return;
        }

        synchronized (lightToObey) {
            while (lightToObey.getCurrentState() == LightState.RED || lightToObey.getCurrentState() == LightState.YELLOW) {
                if (lightToObey.getCurrentState() == LightState.YELLOW && shouldProceedOnYellow()) {
                    System.out.println(carId + " avançando no AMARELO em " + intersection.getId() + " vindo de " + this.lastArrivalDirection);
                    lightToObey.carPassedOnYellow();
                    break; // Sai do loop e prossegue
                }
                try {
                    System.out.println(carId + " aguardando VERDE em " + intersection.getId() + " (Semáforo: " + lightToObey.getCurrentState() + " para " + this.lastArrivalDirection + ")");
                    lightToObey.wait(5000); // Aguarda notificação ou timeout
                    if (lightToObey.getCurrentState() == LightState.RED) { // Ainda vermelho após timeout, continua esperando
                        System.out.println(carId + " ainda VERMELHO em " + intersection.getId() + " para " + this.lastArrivalDirection + ". Continuando a aguardar.");
                    }
                } catch (InterruptedException e) {
                    System.out.println(carId + " interrompido enquanto aguardava no semáforo.");
                    Thread.currentThread().interrupt();
                    running = false;
                    runningSim = false;
                    return;
                }
            }
        }
        System.out.println(carId + " avançando por " + intersection.getId() + " (Semáforo estava " + lightToObey.getCurrentState() + " para " + this.lastArrivalDirection + ")");
        chooseNextStreetAndDepart(intersection);
    }

    private boolean shouldProceedOnYellow() {
        // Simple logic: 50% chance to proceed if yellow
        // More complex logic could consider distance to intersection, speed, etc.
        return random.nextBoolean();
    }

    protected void chooseNextStreetAndDepart(Intersection fromIntersection) {
        // fromIntersection é o cruzamento em que o carro está atualmente e de onde está partindo.
        // this.lastArrivalDirection é a direção que o carro usou para chegar em fromIntersection.
        // this.previousStreet (campo) é a rua usada para chegar em fromIntersection.

        List<Street> possibleExits = grid.getExitingStreetsFromIntersection(fromIntersection, this.lastArrivalDirection);

        if (possibleExits.isEmpty()) {
            System.out.println(getCarId() + " não encontrou saídas válidas de " + fromIntersection.getId() + " (chegou de " + this.lastArrivalDirection + "). Terminando jornada.");
            running = false;
            runningSim = false;
            return;
        }

        Street nextChosenStreet = possibleExits.get(random.nextInt(possibleExits.size()));
        
        // O campo previousStreet do carro foi corretamente definido em arriveAtIntersection para a rua que ele acabou de atravessar para chegar em fromIntersection.
        // Agora, o carro está se movendo para nextChosenStreet.
        this.currentStreet = nextChosenStreet;
        this.positionOnStreet = 0;
        // currentIntersection está sendo deixado, então se torna nulo para o estado do carro enquanto estiver no segmento da rua.
        Intersection departingIntersection = this.currentIntersection; // Deve ser o mesmo que fromIntersection
        this.currentIntersection = null;

        // Determina o próximo cruzamento de destino com base na nova currentStreet
        List<Intersection> connectedIntersections = grid.getIntersectionsConnectedToStreet(this.currentStreet);
        Intersection potentialNextTarget = null;

        if (connectedIntersections.isEmpty()) {
            // A rua não leva a lugar nenhum (ex: um segmento de rua isolado, não deve acontecer em uma configuração de grid válida para simulação)
            potentialNextTarget = null;
        } else if (connectedIntersections.size() == 1) {
            Intersection onlyConnectedInt = connectedIntersections.get(0);
            // Verifica se este único cruzamento conectado é aquele de onde estamos partindo
            if (departingIntersection != null && onlyConnectedInt.getId().equals(departingIntersection.getId())) {
                // Sim, ele só conecta de volta para onde viemos.
                // Se a rua for de mão única, isso implica que é uma saída do sistema em relação a departingIntersection.
                // Se for de mão dupla, é um beco sem saída; o carro faria um retorno e voltaria para departingIntersection.
                if (!this.currentStreet.isTwoWay()) {
                    potentialNextTarget = null; // Rua de mão única levando para fora do sistema
                } else {
                    potentialNextTarget = onlyConnectedInt; // Beco sem saída de mão dupla, o objetivo é retornar
                }
            } else {
                // Conecta a um único cruzamento diferente. Este é o alvo.
                potentialNextTarget = onlyConnectedInt;
            }
        } else {
            // A rua conecta a múltiplos cruzamentos (tipicamente dois para uma rua de passagem).
            // Encontra aquele que NÃO é o departingIntersection.
            for (Intersection connInt : connectedIntersections) {
                if (departingIntersection != null && !connInt.getId().equals(departingIntersection.getId())) {
                    potentialNextTarget = connInt;
                    break;
                }
            }
            // Se potentialNextTarget ainda for nulo aqui, significa que todos os cruzamentos conectados são o departingIntersection
            // (ex: uma rua em loop que só se conecta a si mesma através do departingIntersection, ou erro na configuração do grid).
            // Ou, departingIntersection era nulo (não deveria acontecer aqui).
            if (potentialNextTarget == null && departingIntersection != null) {
                 // Isso implica que a rua pode ser um loop que só se conecta a fromIntersection em ambas as extremidades,
                 // ou todas as outras conexões foram filtradas. Se for uma rua de mão dupla, permite o retorno.
                 if (this.currentStreet.isTwoWay()) {
                    potentialNextTarget = departingIntersection; // Permite retorno/reentrada se for de mão dupla
                 } else {
                    // Rua de mão única que parece levar apenas de volta. Isso provavelmente é uma saída ou erro de configuração.
                    potentialNextTarget = null;
                 }
            }
        }
        
        this.nextIntersection = potentialNextTarget;

        String departingId = (departingIntersection != null) ? departingIntersection.getId() : "PONTO_DE_PARTIDA_DESCONHECIDO";
        if (this.nextIntersection != null) {
            System.out.println(getCarId() + " partiu de " + departingId + ", agora na rua " + this.currentStreet.getId() + " indo em direção a " + this.nextIntersection.getId());
        } else {
            System.out.println(getCarId() + " partiu de " + departingId + ", agora na rua " + this.currentStreet.getId() + " mas sem próximo cruzamento definido. Terminando jornada.");
            running = false;
            runningSim = false;
        }
    }

    public void stopCar() {
        running = false;
        runningSim = false; // Car is no longer active in simulation
        this.interrupt(); // Interrupt if it's sleeping/waiting
    }

    public boolean isRunningSim() { // Added for Simulator
        return runningSim;
    }

    public Street getCurrentStreet() {
        return currentStreet;
    }

    public Street getPreviousStreet() { // Added for Grid logic
        return previousStreet;
    }

    public void setPreviousStreet(Street street) { // Added for Grid logic
        this.previousStreet = street;
    }

    public Intersection getCurrentIntersection() {
        return currentIntersection;
    }

    public double getPositionOnStreet() {
        return positionOnStreet;
    }

    protected Grid getGrid() { // Added for EmergencyVehicle to access grid
        return grid;
    }

    protected void setCurrentStreet(Street street) { // Added for EmergencyVehicle
        this.currentStreet = street;
    }

    protected void setCurrentIntersection(Intersection intersection) { // Added for EmergencyVehicle
        this.currentIntersection = intersection;
    }

    protected void setPositionOnStreet(double position) { // Added for EmergencyVehicle
        this.positionOnStreet = position;
    }

    // This is a placeholder. Grid should determine this based on car's previous street and intersection geometry.
    // For now, it's set when car arrives at intersection.
    // public void setArrivalDirection(Direction dir) { this.arrivalDirection = dir; }
}

