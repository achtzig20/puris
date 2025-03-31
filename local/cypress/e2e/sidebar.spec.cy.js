describe('sidebar', () => {
    beforeEach(() => {
        cy.visit('/');
    });

    it('shows 7 menu items and one is selected', () => {
        cy.getByTestId('sidebar').should('exist');
        cy.getByTestIdContains('sidebar-menu-item').should('have.length', 7);
        cy.get('[data-testid*="sidebar-menu-item"][aria-selected="true"]').should('have.length', 1);
        
        for(let i=0; i < 6; i++) {
          cy.get('[data-testid*="sidebar-menu-item"] a').should('have.length', 6).eq(i).click();
        }
        
        cy.get('[data-testid="sidebar-item-license"]').should('exist');
        cy.get('[data-testid="sidebar-item-license"] a').click();
        cy.url().should('match', /\/aboutLicense$/);
        cy.get('[data-testid*="sidebar-menu-item"][aria-selected="true"]').should('not.exist');
    });
});
