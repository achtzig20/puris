const { defineConfig } = require("cypress");
const fs = require('fs');

module.exports = defineConfig({
    e2e: {
        baseUrl: "http://127.0.0.1:3000",
        viewportWidth: 1920,
        viewportHeight: 1080,
        setupNodeEvents(on) {
          on('after:spec', (spec, results) => {
            if (results && results.video) {
              const failures = results.tests.some((test) =>
                test.attempts.some((attempt) => attempt.state === 'failed')
              )
              if (!failures) {
                fs.unlinkSync(results.video)
              }
            }
          })
        },
    },
    video: true,
});
