package com.simuladortrafego;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class Grid {
    private List<Street> streets;
    private List<Intersection> intersections;
    // Adjacency list for intersections: Intersection ID -> List of connected Street IDs
    private Map<String, Set<String>> intersectionStreetMap;
    // Adjacency list for streets: Street ID -> List of connected Intersection IDs
    private Map<String, Set<String>> streetIntersectionMap;

    public Grid() {
        this.streets = new ArrayList<>();
        this.intersections = new ArrayList<>();
        this.intersectionStreetMap = new HashMap<>();
        this.streetIntersectionMap = new HashMap<>();
    }

    public void addStreet(Street street) {
        this.streets.add(street);
        this.streetIntersectionMap.putIfAbsent(street.getId(), new HashSet<>());
    }

    public void addIntersection(Intersection intersection) {
        this.intersections.add(intersection);
        this.intersectionStreetMap.putIfAbsent(intersection.getId(), new HashSet<>());
    }

    // Connect a street to an intersection
    // The direction indicates how the street arrives at or departs from the intersection
    public void connectStreetToIntersection(String streetId, String intersectionId, Direction streetDirectionAtIntersection, boolean isIncoming) {
        Street street = findStreetById(streetId);
        Intersection intersection = findIntersectionById(intersectionId);

        if (street == null || intersection == null) {
            System.err.println("Erro ao conectar: Rua ou Cruzamento não encontrado.");
            return;
        }

        // Atualiza listas de adjacência
        this.intersectionStreetMap.get(intersectionId).add(streetId);
        this.streetIntersectionMap.get(streetId).add(intersectionId);

        // Informa o cruzamento sobre a rua conectada
        if (isIncoming) {
            intersection.addIncomingStreet(street, streetDirectionAtIntersection);
        } else {
            intersection.addOutgoingStreet(street, streetDirectionAtIntersection);
        }
        System.out.println("Rua conectada " + streetId + (isIncoming ? " chegando em" : " saindo de") + " cruzamento " + intersectionId + " na direção " + streetDirectionAtIntersection);
    }

    public Street findStreetById(String id) {
        for (Street s : streets) {
            if (s.getId().equals(id)) {
                return s;
            }
        }
        return null;
    }

    public Intersection findIntersectionById(String id) {
        for (Intersection i : intersections) {
            if (i.getId().equals(id)) {
                return i;
            }
        }
        return null;
    }

    public List<Intersection> getIntersections() {
        return new ArrayList<>(intersections);
    }

    public List<Street> getStreets() {
        return new ArrayList<>(streets);
    }

    public List<Intersection> getIntersectionsConnectedToStreet(Street street) {
        if (street == null || !streetIntersectionMap.containsKey(street.getId())) {
            return new ArrayList<>();
        }
        List<Intersection> connected = new ArrayList<>();
        for (String intersectionId : streetIntersectionMap.get(street.getId())) {
            Intersection i = findIntersectionById(intersectionId);
            if (i != null) {
                connected.add(i);
            }
        }
        return connected;
    }

    public List<Street> getStreetsConnectedToIntersection(Intersection intersection) {
        if (intersection == null || !intersectionStreetMap.containsKey(intersection.getId())) {
            return new ArrayList<>();
        }
        List<Street> connected = new ArrayList<>();
        for (String streetId : intersectionStreetMap.get(intersection.getId())) {
            Street s = findStreetById(streetId);
            if (s != null) {
                connected.add(s);
            }
        }
        return connected;
    }

    // Method to determine the direction from which a car arrives at an intersection, given its previous street.
    public Direction getArrivalDirection(Car car, Intersection intersection, Street previousStreet) {
        if (previousStreet == null) {
            // This might happen if the car starts directly at an intersection or if previousStreet was not set.
            // Attempt a more generic (but less reliable) lookup if car is already considered at intersection.
            if (car.getCurrentIntersection() != null && car.getCurrentIntersection().equals(intersection)) {
                 // Try to find which incoming street of the intersection the car *might* have come from.
                // Esta é uma alternativa e menos robusta.
                for (Map.Entry<Direction, List<Street>> entry : intersection.getIncomingStreets().entrySet()) {
                    // Esta verificação é problemática se a currentStreet do carro for nula (o que é quando está em um cruzamento)
                    // Precisamos de um link mais definitivo ou o carro precisa lembrar seu ponto/direção de entrada.
                    // Por enquanto, este caminho tem alta probabilidade de falhar ou estar incorreto.
                }
            }
            System.err.println("Erro em getArrivalDirection: Carro " + car.getCarId() + " no Cruzamento " + intersection.getId() + " - ruaAnterior é nula. Não é possível determinar a direção de chegada de forma confiável.");
            return null;
        }

        // Itera sobre as ruas de chegada do cruzamento e suas direções de chegada definidas.
        // A chave do mapa incomingStreets em Intersection é a Direção DE ONDE o tráfego chega.
        for (Map.Entry<Direction, List<Street>> entry : intersection.getIncomingStreets().entrySet()) {
            if (entry.getValue().contains(previousStreet)) {
                // Se a ruaAnterior estiver listada sob uma direção de chegada específica para este cruzamento,
                // essa direção é a nossa resposta.
                return entry.getKey();
            }
        }

        System.err.println("Erro em getArrivalDirection: Não foi possível determinar a direção de chegada para o Carro " + car.getCarId() + 
                           " da Rua " + previousStreet.getId() + " para o Cruzamento " + intersection.getId() + ". Verifique as conexões do grid.");
        return null; // Não deve acontecer se o grid estiver configurado corretamente e a ruaAnterior for válida.
    }

    public List<Street> getExitingStreetsFromIntersection(Intersection intersection, Direction arrivalDirection) {
        List<Street> exits = new ArrayList<>();
        if (intersection == null) return exits;

        // Get all streets connected to the intersection
        Set<String> connectedStreetIds = intersectionStreetMap.get(intersection.getId());
        if (connectedStreetIds == null) return exits;

        for (String streetId : connectedStreetIds) {
            Street street = findStreetById(streetId);
            if (street == null) continue;

            // Logic to determine if this street is a valid exit:
            // 1. It's not the street the car just arrived from (unless it's a dead-end/cul-de-sac scenario, complex).
            // 2. If it's one-way, the car must enter it in the correct direction.
            // This requires knowing the orientation of the street relative to the intersection.

            // Simplified: add all streets that are not the arrival street (if arrivalDirection and street are known)
            // This needs to be much more robust based on how streets are defined as incoming/outgoing in Intersection class
            boolean isArrivalStreet = false;
            if (arrivalDirection != null && intersection.getIncomingStreets().get(arrivalDirection) != null && intersection.getIncomingStreets().get(arrivalDirection).contains(street)) {
                isArrivalStreet = true;
            }

            if (!isArrivalStreet) { // Basic rule: don't immediately go back
                // Further check for one-way streets if that info is available here
                // For now, assume all other connected streets are potential exits
                exits.add(street);
            }
        }
        // If no other exits, and arrival street is two-way, it could be an option (e.g. U-turn if allowed)
        // This is too simple. The Intersection's outgoingStreets map should be the primary source.
        return intersection.getPossibleExits(arrivalDirection); // Delegate to a more robust method in Intersection
    }

    public void printGrid() {
        System.out.println("Configuração do Grid:");
        System.out.println("Cruzamentos:");
        for (Intersection i : intersections) {
            System.out.println("- " + i.getId());
            System.out.println("  Semáforos:");
            for (Map.Entry<Direction, TrafficLight> entry : i.getTrafficLights().entrySet()) {
                System.out.println("    " + entry.getKey() + ": " + entry.getValue().getLightId() + " (" + entry.getValue().getCurrentState() + ")");
            }
            System.out.println("  Ruas Conectadas (Chegada):");
            for (Map.Entry<Direction, List<Street>> entry : i.getIncomingStreets().entrySet()) {
                for (Street s : entry.getValue()) {
                    System.out.println("    " + entry.getKey() + ": " + s.getId());
                }
            }
             System.out.println("  Ruas Conectadas (Saída):");
            for (Map.Entry<Direction, List<Street>> entry : i.getOutgoingStreets().entrySet()) {
                for (Street s : entry.getValue()) {
                    System.out.println("    " + entry.getKey() + ": " + s.getId());
                }
            }
        }
        System.out.println("Ruas:");
        for (Street s : streets) {
            System.out.println("- " + s.getId() + " (Comprimento: " + s.getLength() + ", Mão Dupla: " + s.isTwoWay() + (s.isTwoWay() ? "" : ", Dir: " + s.getDirection()) + ")");
            System.out.println("  Conecta-se aos Cruzamentos:");
            if (streetIntersectionMap.containsKey(s.getId())) {
                for (String intId : streetIntersectionMap.get(s.getId())) {
                    System.out.println("    " + intId);
                }
            }
        }
    }
}

// Extension to Car class needed:
// private Street previousStreet;
// public Street getPreviousStreet() { return previousStreet; }
// public void setPreviousStreet(Street street) { this.previousStreet = street; }
// Update this when car moves from one street to another via an intersection.

// Extension to Intersection class needed:
// public Map<Direction, List<Street>> getIncomingStreets() { return incomingStreets; }
// public Map<Direction, List<Street>> getOutgoingStreets() { return outgoingStreets; }
// public List<Street> getPossibleExits(Direction arrivalDirection) { ... robust logic ... }

