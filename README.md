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

