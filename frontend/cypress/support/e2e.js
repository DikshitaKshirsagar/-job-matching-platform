// Cypress E2E support file
// This file runs before each spec file and is a good place to put
// global configuration and behavior that modifies Cypress.

// Prevent Cypress from failing tests on uncaught exceptions
Cypress.on('uncaught:exception', (err, runnable) => {
  // returning false here prevents Cypress from failing the test
  return false;
});