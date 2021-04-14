# Widget Store Application
A simple application to store widgets in memory or in SQL database.

Written in Java 11 using Spring Boot framework.

## Quick Start
To run application you just need run a usual Spring Boot application using Maven Wrapper:
```shell
./mvnw spring-boot:run
```
This command starts application with embedded HTTP server on TCP port 8080 by default.

The application uses default Spring and Spring Boot configurations.

### Profiles
The application includes 2 types of storages: in-memory and SQL database. It's using Spring Profiles to switch between 
these storages. To activate one of them you need to set an active Spring profile:
```shell
./mvnw spring-boot:run -Dspring.profiles.active=in-memory
./mvnw spring-boot:run -Dspring.profiles.active=database
```
Or via `./src/main/resources/application.properties`:
```properties
spring.profiles.active=in-memory # for in-memory storage
spring.profiles.active=database # for SQL database storage
```
By default, it uses `in-memory` storage (set in `application.properties`).

### REST API
The API of the application is designed with REST style in mind.

For validation, it uses JST-303.

It has an Open API documentation provided via SpringDoc and Swagger UI. The documentation is available at
`http://localhost:8080/v3/api-docs`.

## Widget
A Widget is an object on a plane in a Cartesian coordinate system that has coordinates (X, Y), Z-index, width, height, 
last modification date, and a unique identifier. X, Y, and Z-index are integers (may be negative). Width and height 
are integers > 0. Widget attributes should be not null. 

A Z-index is a unique sequence common to all widgets that determines the order of widgets (regardless of their 
coordinates). Gaps are allowed. The higher the value, the higher the widget lies on the plane.

## Implementation notes
### In-memory storage
To achieve atomicity and thread-safety in the in-memory implementation the application uses pessimistic Read-Write 
locks. So, the write-lock provides exclusive access for all modification operations. When the write-lock is held, no 
other thread can't read or write any data from/to the storage. So, intermediate state is not visible for other threads.
On the other hand, read-lock provides shared access for all read operations. So, multiple threads can read some data 
from the storage simultaneously while no one thread can't do any modifications. This approach is easy to implements and 
has small costs of the synchronization with respect to the performance. 

Other approaches like optimistic locking or MVCC (multi version concurrency control) seem to be much harder to 
implement and not giving a huge performance boost.

#### Pagination
The in-memory (and database too) implementation uses a cursor based pagination. Every `GET/widgets` response contains
a `paging` parameter that describes metadata for paging. If parameter `hasMore` has value `true`, then additional 
parameter `cursor` is provided. Cursor has no meaningful semantic for the client and SHOULD be used only for pagination.
In fact, the value of the cursor is Z-index of the last returned widget. So, if the request contains a `cursor` 
parameter, then the server will return all widgets with `Z-index > cursor`.

This approach allows missed and duplicated widgets in the responses only if some modification have been occurred between 
page requests. However, it's considered as not a big problem because:
* duplicates can be tracked via some kind of hash-map on the client side
* all modifications should be received via server-sent events (WebSockets, HTTP/2, etc.)

So, if using server-sent events, we have to return only unchanged widgets. Cursor based pagination does it efficiently 
both for in-memory and SQL storages.

> Other pagination implementations
> 
> Another implementation of pagination is available in the branch `compliations/pagination-without-misses`. This 
> implementation avoids missed widgets but allows duplicates. However, it is much more complex to understand and maintain 
> and less efficient compared to the simple cursor based pagination.

#### Spatial Search
The in-memory implementation uses R-tree for spatial search. It has *O(M logN)* time complexity for search 
(where `M` is the maximum number of children of each tree's node and `N` is the total number of elements stored in a 
tree). In fact, `M` is considered as constant.

The implementation of R-tree is copy-pasted from 
[there](https://github.com/rweeks/util/blob/master/src/com/newbrightidea/util/RTree.java). Additionally, some tweaks are
added to the initial implementation to support pagination and sorting by Z-index.

### SQL Database implementation
By default, it uses an in-memory H2 Database as an RDBMS. The application works with two tables: `widget` 
(stores widgets' data) and `widget_lock` (for pessimistic locking). All DDL changes are applied via Liquibase patches 
(located in `./src/main/resources/db/changelog`) automatically.

To handle concurrency right, this implementation uses a pessimistic locking of the whole `widget` table for write 
operations. It's like a Serializable isolation level of transactions. The explicit table lock is implemented via 
selecting a particular record from the `widget_lock` table for update (due to lack of explicit table locks support in 
H2). Read operations rely on concurrency control in the database and Read Committed isolation level of transactions. So,
there is no explicit synchronization for read operations. It's not the most efficient way to handle concurrency but due 
to the point that there are much more reads than writes this approach seems OK.

#### Paging
This implementation uses the same approach for pagination as in in-memory. Cursor based pagination is done efficient 
because of the B-tree index on the `widget.z_index` column.

#### Spatial search
The implementation uses H2's specific datatype `GEOMETRY` for storing boundaries of the widget. The `widget` table is 
denormalized for simple usage of separate columns (`x`, `y`, `width`, `height`). Also, it uses an H2's specific 
`SPATIAL INDEX` on the `boundaries` column (of type `GEOMETRY`). Due to lack of the *contains* operator support for 
spatial indices in H2, search query checks this conditions explicitly. Usage of such index helps us to reduce the size 
of the result set for filtering.   