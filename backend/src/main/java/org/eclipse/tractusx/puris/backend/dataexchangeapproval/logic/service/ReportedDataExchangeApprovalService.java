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
import java.util.UUID;
import java.util.function.Function;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.ReportedDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.repository.ReportedDataExchangeApprovalRepository;
import org.springframework.stereotype.Service;

@Service
public class ReportedDataExchangeApprovalService extends DataExchangeApprovalService<ReportedDataExchangeApproval> {
    private final ReportedDataExchangeApprovalRepository repository;
    protected final Function<ReportedDataExchangeApproval, Boolean> validator;

    public ReportedDataExchangeApprovalService(ReportedDataExchangeApprovalRepository repository) {
        this.repository = repository;
        this.validator = this::validate;
    }

    public final ReportedDataExchangeApproval findByOwnDataExchangeRequestId(UUID ownDataExchangeRequestId) {
        return repository.findAll().stream().filter(request -> request.getDataExchangeRequest().getUuid().equals(ownDataExchangeRequestId))
                .findFirst().orElse(null);
    }

    public final ReportedDataExchangeApproval create(ReportedDataExchangeApproval reportedDataExchangeApproval) {
        if (reportedDataExchangeApproval == null || !validator.apply(reportedDataExchangeApproval)) {  
            throw new IllegalArgumentException("Invalid data exchange approval");
        }
        if (repository.findAll().stream().filter(existing -> existing.equals(reportedDataExchangeApproval)).findFirst().isPresent()) {
            throw new KeyAlreadyExistsException("Reported data exchange approval already exists");
        }
        
        if (repository.findAll().stream().anyMatch(d -> d.getRequestId().equals(reportedDataExchangeApproval.getRequestId()))) {
            throw new KeyAlreadyExistsException("Data exchange approval already exists");
        }
        if (reportedDataExchangeApproval.getRequestId() == null) {
            reportedDataExchangeApproval.setRequestId(UUID.randomUUID());
        }
        return repository.save(reportedDataExchangeApproval);
    }

    public final ReportedDataExchangeApproval update(ReportedDataExchangeApproval reportedDataExchangeApproval) {
        if (!validator.apply(reportedDataExchangeApproval)) {
            throw new IllegalArgumentException("Invalid Approval");
        }
        if (reportedDataExchangeApproval.getUuid() == null || repository.findById(reportedDataExchangeApproval.getUuid()).isEmpty()) {
            return null;
        }
        return repository.save(reportedDataExchangeApproval);
    }

    public boolean validate(ReportedDataExchangeApproval dataExchangeApproval) {
        return dataExchangeApproval != null && 
        basicValidation(dataExchangeApproval) &&
        dataExchangeApproval.getDataExchangeRequest() != null;
    }
}
