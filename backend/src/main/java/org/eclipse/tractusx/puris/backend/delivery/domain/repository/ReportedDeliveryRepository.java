package org.eclipse.tractusx.puris.backend.delivery.domain.repository;

import java.util.UUID;

import org.eclipse.tractusx.puris.backend.delivery.domain.model.ReportedDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportedDeliveryRepository extends JpaRepository<ReportedDelivery, UUID> {

}
