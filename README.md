# The FinTech Canary

This came up as a part of my interest in a particular FinTech.
The constraints:

- Bare Functional Java.
- Some fundamental engineering culture.

## The Setup

And, the request is:

- Consume this API: GET https://api.pagerduty.com/users
- Page it.
- Produce production quality code.

## Stumping Grounds

I was stumped by this request. And there are several reasons for that:

- It is for lead engineering position.
- No production quality code will roll its own client.
    - The standard is mature, well weathered and unchanging;
    - Many libraries exist that seamlessly handle complexities like paging;
    - Mature libraries like Ktor, OkHttp, and WebClient (Spring) exist.

Nevertheless I got intrigued by the HttpClient which doesn't share DSL with any of the mainstream libraries today.

## The Solution

First questions I had:

1. Does this "public API" even have a proper scheme - I asked this question and was told "haven't heard of OpenAPI or Swagger"
2. Is this API even paging? -- I thought to look because of the paging questions at the end -- rather trivial.
3. What actual Auth/Auth mechanism is used, in the UX docs it's a simple token as in 1999.
4. What is their versioning scheme and policy? How stable is the API? What are the "fault guards."

So, I started digging and came up with the following:

1. Yes!, in fact. Surprising. The schema is here: https://github.com/PagerDuty/api-schema
2. Yes, it is paging. Hand-rolled by `limit`, `offset`, `more` and `total` fields. And `limit` max is 100. Basically, not following any standard.
3. API Token in header: `Authorization: Token token=your_token_here` is all we got. Account-wide rate limiting at 429, so I have limited tinkering.
4. API v2 has been stable since ~2016. So I will need to think on faults and simulation of one later. Perhaps at the end of my tinkering.

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

1. Added proper full-featured Unit tests:
    1. Stubbed and proxied with Mockito -- didn't like it -- I don't understand the client.
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

**Setup:**

```bash
export PAGERDUTY_API_TOKEN=your_token_here
```

**Run Simple Demo Mode:**


_If gradle installed locally:_

```bash
gradle :api:run
```

OR:

```bash
./gradlew :api:run
```

### Interactive Explorer Mode

```bash
gradle :api:run --args="--interactive"
```

OR:

```bash
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
2. Consider [HAL](https://en.wikipedia.org/wiki/Hypertext_Application_Language) for API design -- this removes the need for API versioning and adds some amazing behavior exportation...

Toodles!


## Followup and final thoughts:

Practice is never futility. But time is precious.
There are few things that I just skimped over in the implementation.
Now that the crunch is over we can handle these in stride by GitHub issues.
Here is the list of things I still remember:

1: Export in the `InteractivePagerDutyCanary` could pick a sensible default folder and format.

2: `PagedResponse.currentPageNumber` will throw `ArithmeticException` when `limit` is 0 and is tested for such. 
This is a common Java paradigm but is bad engineering - Java is full of these. I'd add `if (limit == 0) return 1;`.

3: The `PagerDutyUser.create` is static but returns via constructor - there are patterns for that.

4: `PagerDutyUserService` is lacking any connection pooling. Should create at least one `HttpClient` per service instance.
(Or, use appropriate libraries like OkHttp, WebClient, Ktor, etc., defeating the purpose ofr this exercise.)
Also, there isn't any retry logic implemented. This just goes to show what we get for free in an OSS library.

5: No request/response logging/intercepting/monitoring/crosscutting. This should behove people to NEVER utilize `HttpClient` directly.

6: `EntryPoint` demo, sure, but security is an afterthought both on the client and server sides. Inject runtime secrets if another library is permittable.

7: `PagingUserCanary` serves its purpose as a smoker. but all the hardcoding, like page sizing, is pure cringe hidden only by Java absurd verbosity. Consider manifests.

8: `InteractivePagerDutyCanary` oh, just noticed a resource leak: Scanner never closed. Needs try with resources or better.<br/>
`Thread.sleep` is a dirty hack. Exponential backoff at least would be better.<br/>
CSV export is bare bones with possible injection bugs if data has new lines or delimiters.<br/>
Clearscreen only works on ANSI terminals - we care NOT for Winderz, but with a little bit of time much better is possible.<br/>

**Security Concerns** - a whole issue of its own! Sure, this is just a toy, nobody does things this bad in production.<br/>
But simply being on the web this thang creates bad examples. Don't even bother with tokens for toys OR implement correctly.
I suspect this was just done so people don't abuse the free API for fun. But there are planty who will just copy-paste code they see.<br/>
Should parse `X-RateLimit-*` in he header too.<br/>
Neither input, marshalling/unmarshalling or other parsing is validated.<br/>

**Performance Issues** - yet another massive DON'T for using HttpClient directly.<br/>
a) No catching without some hefty work added.<br/>
b) Synchronous API calls only - lot's can be improved on that.<br/>
c) Large memory footprint with this single brick loaded.<br/>
d) And no connection pooling as already mentioned.

**Personal pet peeve:** I'd written it haphazardly and sporadically while doing other things.<br/>
But the inconsistent use of `var`, for example bothers me. You know what?! It does not! - Just use Kotlin 😜 <br/>
Should extract constants. Jury is out on magic numbers. And some method names are too long - again, Java for you. 

Well, that's about all I can remember.

Toodles!
