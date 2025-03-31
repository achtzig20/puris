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
