package com.campus.service.domain;

import com.campus.service.common.BusinessException;
import com.campus.service.entity.ServiceRequest;
import com.campus.service.entity.Venue;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestRulesTest {
    @Test
    void stateMachineAllowsOnlySpecifiedTransitions() {
        assertTrue(RequestStatus.SUBMITTED.canTransitionTo(RequestStatus.ACCEPTED));
        assertTrue(RequestStatus.SUBMITTED.canTransitionTo(RequestStatus.CANCELLED));
        assertTrue(RequestStatus.ACCEPTED.canTransitionTo(RequestStatus.PROCESSING));
        assertTrue(RequestStatus.ACCEPTED.canTransitionTo(RequestStatus.REJECTED));
        assertTrue(RequestStatus.ACCEPTED.canTransitionTo(RequestStatus.CANCELLED));
        assertTrue(RequestStatus.PROCESSING.canTransitionTo(RequestStatus.FINISHED));
        assertTrue(RequestStatus.FINISHED.canTransitionTo(RequestStatus.EVALUATED));

        assertFalse(RequestStatus.SUBMITTED.canTransitionTo(RequestStatus.PROCESSING));
        assertFalse(RequestStatus.ACCEPTED.canTransitionTo(RequestStatus.FINISHED));
        assertFalse(RequestStatus.REJECTED.canTransitionTo(RequestStatus.ACCEPTED));
        assertFalse(RequestStatus.CANCELLED.canTransitionTo(RequestStatus.ACCEPTED));
        assertFalse(RequestStatus.EVALUATED.canTransitionTo(RequestStatus.FINISHED));
        assertThrows(BusinessException.class,
                () -> RequestRules.requireTransition("SUBMITTED", RequestStatus.FINISHED));
    }

    @Test
    void repairAndCertificateRequireTheirOwnFields() {
        ServiceRequest repair = new ServiceRequest();
        repair.setLocation("1号宿舍 201");
        repair.setRepairCategory("水电");
        repair.setUrgency("普通");
        assertDoesNotThrow(() -> RequestRules.validateRepair(repair));
        repair.setRepairCategory(" ");
        assertThrows(BusinessException.class, () -> RequestRules.validateRepair(repair));

        ServiceRequest certificate = new ServiceRequest();
        certificate.setCertificateType("在读证明");
        certificate.setPurpose("实习");
        certificate.setLanguage("中文");
        certificate.setDeliveryMethod("在线领取");
        certificate.setCopies(1);
        assertDoesNotThrow(() -> RequestRules.validateCertificate(certificate));
        certificate.setCopies(0);
        assertThrows(BusinessException.class, () -> RequestRules.validateCertificate(certificate));
    }

    @Test
    void venueRequiresFutureValidAvailableCapacity() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 10, 10, 0);
        ServiceRequest request = new ServiceRequest();
        request.setEventName("就业分享会");
        request.setContactName("陈同学");
        request.setContactPhone("13800000001");
        request.setAppointmentStart(now.plusHours(1));
        request.setAppointmentEnd(now.plusHours(2));
        request.setAttendeeCount(30);

        Venue venue = new Venue();
        venue.setStatus("AVAILABLE");
        venue.setCapacity(30);
        assertDoesNotThrow(() -> RequestRules.validateVenue(request, venue, now));

        request.setAttendeeCount(31);
        assertThrows(BusinessException.class, () -> RequestRules.validateVenue(request, venue, now));
        request.setAttendeeCount(30);
        request.setAppointmentEnd(request.getAppointmentStart());
        assertThrows(BusinessException.class, () -> RequestRules.validateVenue(request, venue, now));
        request.setAppointmentEnd(now.plusHours(2));
        venue.setStatus("UNAVAILABLE");
        assertThrows(BusinessException.class, () -> RequestRules.validateVenue(request, venue, now));
    }
}
