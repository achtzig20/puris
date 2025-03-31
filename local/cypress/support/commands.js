Cypress.Commands.add('verifyConfidentialityBanner', () => {
  cy.get('[data-testid="confidential-banner"]').should('exist');
})

Cypress.Commands.add('verifyCopyrightFooter', () => {
  cy.contains('Copyright © Catena-X Automotive Network').should('exist');
})

Cypress.Commands.add('getByTestId', (testid) => {
  return cy.get(`[data-testid="${testid}"]`);
})

Cypress.Commands.add('getByTestIdContains', (testid) => {
  return cy.get(`[data-testid*="${testid}"]`);
})
