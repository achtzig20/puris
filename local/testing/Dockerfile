FROM cypress/included:14.2.0

WORKDIR /testing

COPY . .

RUN npm install playwright-webkit --save-dev

RUN npx playwright install-deps webkit
