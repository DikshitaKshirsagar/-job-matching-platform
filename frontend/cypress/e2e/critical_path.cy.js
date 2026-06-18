/// <reference types="cypress" />

/**
 * Critical Path E2E Test
 *
 * Tests the full user journey: register → view jobs → apply → view applications.
 *
 * Prerequisites:
 *   - Backend running on http://localhost:8081
 *   - Frontend running on http://localhost:3000
 *
 * Run with:
 *   cd frontend && npx cypress run --e2e
 *   or
 *   cd frontend && npx cypress open
 */

describe('Critical Path: Register → Jobs → Apply → Applications', () => {
  const testEmail = `e2e_test_${Date.now()}@example.com`;
  const testPassword = 'password123';
  const testName = 'E2E Test User';

  before(() => {
    // Register a new user via API before running tests
    cy.request({
      method: 'POST',
      url: 'http://localhost:8081/api/v1/auth/register',
      headers: { 'Content-Type': 'application/json' },
      body: {
        fullName: testName,
        email: testEmail,
        password: testPassword,
        confirmPassword: testPassword,
        role: 'SEEKER',
      },
    }).then((response) => {
      expect(response.status).to.equal(201);
      cy.wrap(response.body.data.token).as('authToken');
    });
  });

  it('completes full application flow', () => {
    // Step 1: Visit the app
    cy.visit('/');

    // Step 2: Navigate to login/register
    cy.contains('Login', { timeout: 10000 }).click();

    // Step 3: Fill in login form using the registered user
    cy.get('input[name="email"], input[type="email"], input[placeholder*="email" i]')
      .first()
      .type(testEmail);

    cy.get('input[name="password"], input[type="password"]')
      .first()
      .type(testPassword);

    cy.get('button[type="submit"]').click();

    // Step 4: Verify we land on dashboard or home page
    cy.url({ timeout: 10000 }).should('not.include', '/login');

    // Step 5: Navigate to jobs page
    cy.visit('/jobs');

    // Step 6: Verify job cards are visible
    cy.get('[data-cy="job-card"], .job-card, [class*="job"]', { timeout: 10000 })
      .should('exist');

    // Step 7: Try to apply to the first job card
    cy.get('[data-cy="job-card"], .job-card, [class*="job"]')
      .first()
      .within(() => {
        cy.get('[data-cy="apply-btn"], button:contains("Apply"), a:contains("Apply")')
          .click();
      });

    // Step 8: Navigate to applications page
    cy.visit('/applications');

    // Step 9: Verify application rows are visible
    cy.get('[data-cy="application-row"], .application-row, [class*="application"]', { timeout: 10000 })
      .should('exist');
  });
});