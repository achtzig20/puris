/*
Copyright (c) 2026 Volkswagen AG

See the NOTICE file(s) distributed with this work for additional
information regarding copyright ownership.

This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.

SPDX-License-Identifier: Apache-2.0
*/
package org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.service;

import java.util.Date;
import java.util.UUID;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.OwnDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.ReportedDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.adapter.DataExchangeApprovalSammMapper;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.dto.dataexchangeapprovalsamm.DataExchangeApprovalSamm;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DataExchangeApprovalApiService {
    @Autowired
    private PartnerService partnerService;
    @Autowired
    private ReportedDataExchangeApprovalService reportedDataExchangeApprovalService;
    @Autowired
    private EdcAdapterService edcAdapterService;
    @Autowired
    private DataExchangeApprovalSammMapper sammMapper;
    @Autowired
    private ObjectMapper objectMapper;
    
    public static final String DATA_EXCHANGE_APPROVAL_CONTEXT = "CX-DataExchangeRequestAPI-ReceiveApproval:1.0.0";
    public static final String MESSAGE_HEADER_VERSION = "3.0.0";

    public ReportedDataExchangeApproval handleIncomingDataExchangeApproval(String bpnl, DataExchangeApprovalSamm samm) {
        Partner partner = partnerService.findByBpnl(bpnl);
        if (partner == null) {
            log.error("Unknown Partner BPNL");
            return null;
        }
        var approval = sammMapper.sammToReportedDataExchangeApproval(bpnl, samm);
        if (approval == null) {
            log.error("Error mapping incoming Approval");
            return null;
        }
        ReportedDataExchangeApproval existingApproval = null;
        if (approval.getUuid() != null) {
            existingApproval = reportedDataExchangeApprovalService.findByApprovalId(approval.getApprovalId());
        }

        if (existingApproval != null) {
            log.info("Updating existing Approval");
            approval.setUuid(existingApproval.getUuid());
            if (reportedDataExchangeApprovalService.update(approval) == null) {
                log.error("Error updating Approval");
                return null;
            }
            return approval;
        }
        try {
            log.info("Creating new Approval");
            return reportedDataExchangeApprovalService.create(approval);
        } catch (KeyAlreadyExistsException e) {
            log.error("Approval already exists", e);
            return null;
        } catch (IllegalArgumentException e) {
            log.error("Invalid Approval: {}", approval.toString());
            return null;
        }
    }

    public void sendDataExchangeApproval(OwnDataExchangeApproval approval, Partner partner) {
        var body = createDataExchangeApprovalBody(approval);
        try {
            edcAdapterService.doDataExchangeApprovalPostRequest(partner, body);
            log.info("Successfully sent Data Exchange Approval to partner " + partner.getBpnl()); 
        } catch (Exception e) {
            log.error("Error in ReportedDataExchangeApproval for partner " + partner.getBpnl(), e);
        }
    }

    private JsonNode createDataExchangeApprovalBody(OwnDataExchangeApproval approval) {
        var ownPartnerEntity = partnerService.getOwnPartnerEntity();
        var body = objectMapper.createObjectNode();
        var header = objectMapper.createObjectNode();
        body.set("header", header);
        header.put("senderBpn", ownPartnerEntity.getBpnl());
        header.put("receiverBpn", approval.getDataExchangeRequest().getNotification().getPartner().getBpnl());
        header.put("context", DATA_EXCHANGE_APPROVAL_CONTEXT);
        header.put("messageId", UUID.randomUUID().toString());
        header.put("sentDateTime", new Date().toString());
        header.put("version", MESSAGE_HEADER_VERSION);
        var samm = sammMapper.ownDataExchangeApprovalToSamm(approval);
        body.set("content", objectMapper.convertValue(samm, JsonNode.class));
        return body;
    }
}
