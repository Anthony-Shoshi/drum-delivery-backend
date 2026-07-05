package com.drum_delivery_backend.repositories;

import com.drum_delivery_backend.models.ShipmentModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<ShipmentModel, String> {

    Optional<ShipmentModel> findByShipmentNumber(String shipmentNumber);

    List<ShipmentModel> findByInvoiceNo(String invoiceNo);

    List<ShipmentModel> findByBlNo(String blNo);


    List<ShipmentModel> findByStatus(String status);

    List<ShipmentModel> findByDestinationSiteId(String siteId);

    List<ShipmentModel> findByCreationDateBetween(Date startDate, Date endDate);

    List<ShipmentModel> findByExpectedArrivalDateBetween(Date startDate, Date endDate);

    List<ShipmentModel> findByShipmentNumberStartingWith(String prefix);

    /**
     * Update only the documents_paths field for a shipment
     * This is much more efficient than loading the entire entity with all relationships
     */
    @Modifying
    @Query("UPDATE ShipmentModel s SET s.documentsPaths = :documentsPaths WHERE s.shipmentId = :shipmentId")
    int updateDocumentsPaths(@Param("shipmentId") String shipmentId, @Param("documentsPaths") String documentsPaths);

    /**
     * Get only the documents_paths field for a shipment
     * This avoids loading the entire entity with all relationships
     */
    @Query("SELECT s.documentsPaths FROM ShipmentModel s WHERE s.shipmentId = :shipmentId")
    Optional<String> findDocumentsPathsByShipmentId(@Param("shipmentId") String shipmentId);
}