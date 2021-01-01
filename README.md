# Hawaiian pizza

Possible solution for part 2:

The correct approach would be to create a git repo with the initial project sent by Graphtech and then do a feature branch to implement the new rule for Part 2, and the diff with master would provide the difference of my code.
That being said and since the changes are not significant, here they are: 

1 - Altered the file PurchaseServiceTest, given TDD approach to implement new rule.

2 - Altered the file PurchaseService, method computeAmount to accomodate failed TDD tests.

3 - Added to pom file the dependency commons-math3 for convenience.

## How to run locally
The application requires docker to be installed locally (or to install all the dependant services manually). Start up the docker-compose with the file in the root and the app is ready to run.
 
## Sample HTTP requests
There are sample HTTP requests in the root of the project in the `sampleRequests.http` file. The requests contain correct auth headers. They can be ran from the IntelliJ Idea Http client (only part of the paid version).

## Sample data
Default users and pizzas are created using SQL load script. Other data should be created using sample HTTP request

## Authentication
Authentication is based on Basic Auth, so with every request to a protected endpoint, username and password must be added as headers.
