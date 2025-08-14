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

ToDo: