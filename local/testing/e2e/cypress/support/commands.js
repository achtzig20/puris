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

Cypress.Commands.add('selectAutocompleteOption', (testid, option) => {
  cy.getByTestId(testid).click();
  cy.get('.MuiAutocomplete-popper').contains(option).should('exist').click();
  cy.getByTestId(testid).get(`input[value="${option}"]`).should('exist');
})

Cypress.Commands.add('clearAutocompleteSelection', (testid) => {
  cy.getByTestId(testid).find('input').clear();
  // clearing the input opens the dropdown -> click to close
  cy.getByTestId(testid).click();
})

Cypress.Commands.add('selectRelativeDate', (testid, dateOffset) => {
  cy.getByTestId(testid).find('[aria-label="Choose date"]').click();
  cy.get('[role="rowgroup"] button').each((button, index, list) => {
      const backgroundColor = window.getComputedStyle(button[0]).backgroundColor;
      if (backgroundColor === 'rgb(147, 147, 147)') {
          cy.wrap(list[index + dateOffset]).click();
      }
  });
})

Cypress.Commands.add('login', (role) => {
  cy.session([role], () => {
    if (Cypress.env('idp_enabled')) {
      if (role === 'supplier') {
        cy.origin(Cypress.env('supplierUrl'), () => {
          cy.visit('/materials');
        })
      } else {
        cy.visit('/materials');
      }
      cy.origin(Cypress.env('central_idp_url'), { args: { role }}, ({ role }) => {
        cy.contains(Cypress.env(role).company_name).click();
      });
      cy.origin(Cypress.env('shared_idp_url'), { args: { role }}, ({ role }) => {
        cy.get('#username').should('exist').type(Cypress.env(role).username);
        cy.get('#password').should('exist').type(Cypress.env(role).password);
        cy.get('#kc-login').should('exist').click();
      });
    }
  });
})
