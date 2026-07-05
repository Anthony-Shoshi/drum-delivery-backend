package com.drum_delivery_backend.services;

import com.drum_delivery_backend.models.DrumModel;
import com.drum_delivery_backend.models.OrderModel;
import com.drum_delivery_backend.models.ShipmentModel;
import com.drum_delivery_backend.models.TruckDeliveryModel;
import com.drum_delivery_backend.repositories.DrumRepository;
import com.drum_delivery_backend.repositories.OrderRepository;
import com.drum_delivery_backend.repositories.ShipmentRepository;
import com.drum_delivery_backend.repositories.TruckDeliveryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TruckDeliveryService {

    private final TruckDeliveryRepository truckDeliveryRepository;
    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final DrumRepository drumRepository;

    @Autowired
    public TruckDeliveryService(TruckDeliveryRepository truckDeliveryRepository, ShipmentRepository shipmentRepository, OrderRepository orderRepository, DrumRepository drumRepository) {
        this.truckDeliveryRepository = truckDeliveryRepository;
        this.shipmentRepository = shipmentRepository;
        this.orderRepository = orderRepository;
        this.drumRepository = drumRepository;
    }

    public List<TruckDeliveryModel> getAllTruckDeliveries() {
        return truckDeliveryRepository.findAll();
    }

    public Optional<TruckDeliveryModel> getTruckDeliveryById(String id) {
        return truckDeliveryRepository.findById(id);
    }

    public List<TruckDeliveryModel> getTruckDeliveriesByShipment(String shipmentId) {
        return truckDeliveryRepository.findByShipmentId(shipmentId);
    }

    public List<TruckDeliveryModel> getTruckDeliveriesByStatus(String status) {
        return truckDeliveryRepository.findByStatus(status);
    }

    public List<TruckDeliveryModel> getTruckDeliveriesByTruckNumber(String truckNumber) {
        return truckDeliveryRepository.findByTruckNumber(truckNumber);
    }

    public List<TruckDeliveryModel> getTruckDeliveriesByScheduledDate(Date startDate, Date endDate) {
        return truckDeliveryRepository.findByScheduledDateBetween(startDate, endDate);
    }

    public List<TruckDeliveryModel> getTruckDeliveriesByOrder(String orderId) {
        return truckDeliveryRepository.findByOrderId(orderId);
    }

    @Transactional
    public TruckDeliveryModel createTruckDelivery(TruckDeliveryModel truckDelivery) {
        if (truckDelivery.getId() == null || truckDelivery.getId().isEmpty()) {
            truckDelivery.setId(UUID.randomUUID().toString());
        }
        
        // Validate shipment exists
        if (truckDelivery.getShipment() != null && truckDelivery.getShipment().getId() != null) {
            Optional<ShipmentModel> shipment = shipmentRepository.findById(truckDelivery.getShipment().getId());
            if (shipment.isEmpty()) {
                throw new IllegalArgumentException("Shipment not found with ID: " + truckDelivery.getShipment().getId());
            }
            truckDelivery.setShipment(shipment.get());
        } else {
            throw new IllegalArgumentException("Shipment is required for creating a truck delivery");
        }
        
        // Set creation date if not provided
        if (truckDelivery.getCreationDate() == null) {
            truckDelivery.setCreationDate(new Date());
        }
        
        // Set status if not provided
        if (truckDelivery.getStatus() == null || truckDelivery.getStatus().isEmpty()) {
            truckDelivery.setStatus("SCHEDULED");
        }
        
        return truckDeliveryRepository.save(truckDelivery);
    }

    @Transactional
    public Optional<TruckDeliveryModel> updateTruckDelivery(String id, TruckDeliveryModel truckDeliveryDetails) {
        return truckDeliveryRepository.findById(id)
                .map(existingTruckDelivery -> {
                    if (truckDeliveryDetails.getTruckNumber() != null) {
                        existingTruckDelivery.setTruckNumber(truckDeliveryDetails.getTruckNumber());
                    }
                    if (truckDeliveryDetails.getDriverName() != null) {
                        existingTruckDelivery.setDriverName(truckDeliveryDetails.getDriverName());
                    }
                    if (truckDeliveryDetails.getDriverPhone() != null) {
                        existingTruckDelivery.setDriverPhone(truckDeliveryDetails.getDriverPhone());
                    }
                    if (truckDeliveryDetails.getLicensePlate() != null) {
                        existingTruckDelivery.setLicensePlate(truckDeliveryDetails.getLicensePlate());
                    }
                    if (truckDeliveryDetails.getScheduledDate() != null) {
                        existingTruckDelivery.setScheduledDate(truckDeliveryDetails.getScheduledDate());
                    }
                    if (truckDeliveryDetails.getActualDepartureDate() != null) {
                        existingTruckDelivery.setActualDepartureDate(truckDeliveryDetails.getActualDepartureDate());
                    }
                    if (truckDeliveryDetails.getActualArrivalDate() != null) {
                        existingTruckDelivery.setActualArrivalDate(truckDeliveryDetails.getActualArrivalDate());
                    }
                    if (truckDeliveryDetails.getStatus() != null) {
                        existingTruckDelivery.setStatus(truckDeliveryDetails.getStatus());
                    }
                    if (truckDeliveryDetails.getNotes() != null) {
                        existingTruckDelivery.setNotes(truckDeliveryDetails.getNotes());
                    }
                    
                    return truckDeliveryRepository.save(existingTruckDelivery);
                });
    }

    @Transactional
    public boolean deleteTruckDelivery(String id) {
        return truckDeliveryRepository.findById(id)
                .map(truckDelivery -> {
                    truckDeliveryRepository.delete(truckDelivery);
                    return true;
                }).orElse(false);
    }

    @Transactional
    public Optional<TruckDeliveryModel> addOrderToTruckDelivery(String truckDeliveryId, String orderId) {
        Optional<TruckDeliveryModel> truckDeliveryOpt = truckDeliveryRepository.findById(truckDeliveryId);
        Optional<OrderModel> orderOpt = orderRepository.findById(orderId);
        
        if (truckDeliveryOpt.isPresent() && orderOpt.isPresent()) {
            TruckDeliveryModel truckDelivery = truckDeliveryOpt.get();
            OrderModel order = orderOpt.get();
            
            // Validate that order belongs to the same shipment
            if (order.getShipment() == null || 
                !order.getShipment().getId().equals(truckDelivery.getShipment().getId())) {
                throw new IllegalArgumentException("Order must belong to the same shipment as the truck delivery");
            }
            
            truckDelivery.addOrderId(order.getId());
            return Optional.of(truckDeliveryRepository.save(truckDelivery));
        }
        
        return Optional.empty();
    }

    @Transactional
    public Optional<TruckDeliveryModel> removeOrderFromTruckDelivery(String truckDeliveryId, String orderId) {
        Optional<TruckDeliveryModel> truckDeliveryOpt = truckDeliveryRepository.findById(truckDeliveryId);
        
        if (truckDeliveryOpt.isPresent()) {
            TruckDeliveryModel truckDelivery = truckDeliveryOpt.get();
            
            if (truckDelivery.getOrderIds().contains(orderId)) {
                truckDelivery.removeOrderId(orderId);
                return Optional.of(truckDeliveryRepository.save(truckDelivery));
            }
        }
        
        return Optional.empty();
    }

    @Transactional
    public Optional<TruckDeliveryModel> addDrumToTruckDelivery(String truckDeliveryId, String drumNumber) {
        Optional<TruckDeliveryModel> truckDeliveryOpt = truckDeliveryRepository.findById(truckDeliveryId);
        Optional<DrumModel> drumOpt = drumRepository.findById(drumNumber);
        
        if (truckDeliveryOpt.isPresent() && drumOpt.isPresent()) {
            TruckDeliveryModel truckDelivery = truckDeliveryOpt.get();
            DrumModel drum = drumOpt.get();
            
            // Validate that drum belongs to the same shipment
            if (drum.getShipment() == null || 
                !drum.getShipment().getId().equals(truckDelivery.getShipment().getId())) {
                throw new IllegalArgumentException("Drum must belong to the same shipment as the truck delivery");
            }
            
            // Check if drum is already assigned to another truck delivery
            if (drum.getTruckDelivery() != null && !drum.getTruckDelivery().getId().equals(truckDeliveryId)) {
                throw new IllegalArgumentException("Drum is already assigned to another truck delivery");
            }
            
            truckDelivery.addDrum(drum);
            return Optional.of(truckDeliveryRepository.save(truckDelivery));
        }
        
        return Optional.empty();
    }

    @Transactional
    public Optional<TruckDeliveryModel> removeDrumFromTruckDelivery(String truckDeliveryId, String drumNumber) {
        Optional<TruckDeliveryModel> truckDeliveryOpt = truckDeliveryRepository.findById(truckDeliveryId);
        Optional<DrumModel> drumOpt = drumRepository.findById(drumNumber);
        
        if (truckDeliveryOpt.isPresent() && drumOpt.isPresent()) {
            TruckDeliveryModel truckDelivery = truckDeliveryOpt.get();
            DrumModel drum = drumOpt.get();
            
            if (truckDelivery.containsDrum(drumNumber)) {
                truckDelivery.removeDrum(drum);
                return Optional.of(truckDeliveryRepository.save(truckDelivery));
            }
        }
        
        return Optional.empty();
    }

    @Transactional
    public Optional<TruckDeliveryModel> assignMultipleDrumsToTruckDelivery(String truckDeliveryId, List<String> drumNumbers) {
        Optional<TruckDeliveryModel> truckDeliveryOpt = truckDeliveryRepository.findById(truckDeliveryId);
        
        if (truckDeliveryOpt.isPresent()) {
            TruckDeliveryModel truckDelivery = truckDeliveryOpt.get();
            
            for (String drumNumber : drumNumbers) {
                Optional<DrumModel> drumOpt = drumRepository.findById(drumNumber);
                if (drumOpt.isPresent()) {
                    DrumModel drum = drumOpt.get();
                    
                    // Validate that drum belongs to the same shipment
                    if (drum.getShipment() == null || 
                        !drum.getShipment().getId().equals(truckDelivery.getShipment().getId())) {
                        throw new IllegalArgumentException("Drum " + drumNumber + " must belong to the same shipment as the truck delivery");
                    }
                    
                    // Check if drum is already assigned to another truck delivery
                    if (drum.getTruckDelivery() != null && !drum.getTruckDelivery().getId().equals(truckDeliveryId)) {
                        throw new IllegalArgumentException("Drum " + drumNumber + " is already assigned to another truck delivery");
                    }
                    
                    truckDelivery.addDrum(drum);
                }
            }
            
            return Optional.of(truckDeliveryRepository.save(truckDelivery));
        }
        
        return Optional.empty();
    }
}