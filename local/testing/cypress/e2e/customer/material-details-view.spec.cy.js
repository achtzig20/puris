/*
Copyright (c) 2025 Volkswagen AG
Copyright (c) 2025 Contributors to the Eclipse Foundation

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

describe('material details view', () => {
    it('exists and contains all necesseray components for all test materials', () => {
        cy.fixture('materials.json').then((materials) => {
            materials.forEach((material) => {
                const directions = [];
                if (material.materialFlag) directions.push('Inbound');
                if (material.productFlag) directions.push('Outbound');

                directions.forEach((direction) => {
                    cy.visit(`/materials/${direction.toLowerCase()}/${material.ownMaterialNumber}`);
                    
                    cy.verifyConfidentialityBanner();
                    cy.verifyCopyrightFooter();

                    cy.getByTestId('back-button').should('exist');

                    const informationType = direction === 'Inbound' ? 'Demand' : 'Production';
                    cy.contains(`${informationType} Information for ${material.name} (${direction})`).should('exist');

                    if (direction === 'Inbound') {
                      cy.getByTestId('add-demand-button').should('exist');
                    } else {
                      cy.getByTestId('add-production-button').should('exist');
                    }
                    cy.getByTestId('add-stock-button').should('exist');
                    cy.getByTestId('add-delivery-button').should('exist');
                    cy.getByTestId('schedule-erp-button').should('exist');
                    cy.getByTestId('refresh-partner-data-button').should('exist');

                    cy.getByTestId('own-summary-panel').should('have.length', 2).each(panel => {
                        cy.wrap(panel).contains(direction === 'Inbound' ? 'Material Demand' : 'Planned Production').should('exist');
                        cy.wrap(panel).contains(direction === 'Inbound' ? 'Incoming Deliveries' : 'Outgoing Shipments').should('exist');
                        cy.wrap(panel).contains('Projected Item Stock').should('exist');
                        cy.wrap(panel).contains('Days of Supply').should('exist');
                    });
                    cy.getByTestId('reported-summary-panel').should('not.be.visible');

                    cy.getByTestId('collapsible-summary-button').first().click();
                    cy.get('[data-testid="collapsible-summary-button"] + [data-testid="reported-summary-panel"]').should('be.visible');
                });
            });
        });
    });
});
