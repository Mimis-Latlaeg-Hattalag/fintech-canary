# The FinTech Canary

This came up as a part of my interest ina particular FinTech.
The constraints:

- Bare Functional Java.
- Some fundamental engineering culture.

## The Setup

And, the request is:

- Consume this API: GET https://api.pagerduty.com/users
- Page it.
- Produce production quality code.

## Stumping Grounds

I ws stumped by this request. And there are several reasons for that:

- It is for lead engineering position.
- No production quality code will roll its own client.
  - The standard is mature, well weathered and unchanging;
  - Many libraries exist that seamlessly handle complexities like paging;
  - Mature libraries like Ktor, OkHttp, and WebClient (Spring) exist.

Nevertheless I got intrigued by the HttClient which doesn't share DSL with any of the mainstream libraries today.

## The Solution

First questions I had:

1. Does this "public API" even have a proper scheme - I asked this question and was told "haven't heard of OpenAPI or Swagger"
2. Is this API even paging? -- I thought to look because of the paging questions at the end -- rather trivial.
3. What actual Auth/Auth mechanism is used, in the UX docs it's a simple token as in 1999.
4. What is their versioning scheme and policy? How stable is the API? What are the "fault guards."

So, I started digging and came up with the following:

1. Yes!, in fact. Surprising. The schema is here: https://github.com/PagerDuty/api-schema
2. Yes, it is paging. Hand-rolled by `limit`, `offset`, `more` and `total` fields. And `limit` max ia 100. Basically, not following any standard.
3. API Token in header: `Authorization: Token token=your_token_here` is all we got. Account-wide rate limiting at 429, so I have limited tinkering.
4. API v2 has been stable since ~2016. So i will need to think on faults and simulation of one later. Perhaps at the end of my tinkering.

Also, reading the scheme that I found on their GitHub repo (PagerDuty/api-schema) this domain DOES have a canonical name - `PagerDutyUser`.
So I will implement the model next:

* `PagerDutyUser` -- matching canonical name, introducing required and optional fields.
* `PagedResponse<T>` -- generic to hold my pagination ideas.

### a. Domain Model

1. I used the Model Builder to render the DTOs.
2. Wrote manual tests to validate.
3. Reviewed a few old implementations and borrowed from Spring Rest Template.
4. Jackson `@JsonAnySetter` and Records is a problem; Using `@JsonCreator` - the customer was worried about future API changes.

### b. Implement the simplest service over the real model.

1. Implement Naive Service with that primitive client.
2. Implement basic tests for Java-native pitfalls, but no functional test.

### c. Having the service available and hard-bolted -- implement api entry point and play.

1. Added Canary methods to properly exercise remote API.
2. Bootstrapped `main` with some methods.
3. Added token as ENV variable


Works as expected.

## The refactoring

In Red-Green-Refactor cycle it's time to refine this code.

### d. Added full tests to the service.

1. Added proper ful-featured Unit tests:
   1. Stubbed and proxied with Mokito -- didn't like it -- I don't understand the client.
   2. Added private constructor to the service.
   3. Implemented full stubbing of the client to learn exactly how it's written.
2. Added 2 Pre-Production tests -- client wonders about API changes before release.
   1. Curl
   2. URL

### e. Added Pre-production tests

1. Created tests that validate our implementation against alternative HTTP approaches (URL and curl)
2. Tests run automatically when PAGERDUTY_API_TOKEN is set
3. Validates pagination with various offset/limit combinations

### f. Created Interactive User Explorer

Full-featured canary with:
- Visual pagination with ANSI colors
- Page navigation (next, previous, jump to page)
- Configurable page size
- User search functionality
- Statistics collection (time zones, roles)
- Data export (CSV/JSON)
- Rate limit handling
- Performance metrics

## Usage

### Simple Demo Mode

```bash
export PAGERDUTY_API_TOKEN=your_token_here ./gradlew :api:run
# Then choose option 1
```

### Interactive Explorer Mode
```bash
export PAGERDUTY_API_TOKEN=your_token_here ./gradlew :api:run
# Then choose option 2

# Or directly:
./gradlew :api:run --args="--interactive"
```

### Features of Interactive Mode:
- **Browse Users**: Navigate through pages with detailed user information
- **Search**: Find users by name or email in loaded data
- **Statistics**: View distribution by time zone and role
- **Export**: Save all users to CSV or JSON format
- **Performance Monitoring**: Track API response times

## CONCLUSIONS:

1. Doesn't pay to reinvent the wheel -- I love https://ktor.io/ client instead.
    1. This took 5 hours, and `ktor` client takes 5 minutes and manages paging better than I can.
2. Consider [HAL](https://en.wikipedia.org/wiki/HAL_(software) "Hardware Abstraction Layer ") for API design -- this removes the need for API versioning and adds some amazing behavior exportation...

Toodles!
