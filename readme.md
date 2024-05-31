# Socket Guess Game

This project is a Guess Game Client Server
``Author: 18600355@student.hcmus.edu.vn``
***
# Application Diagram
![alt text](https://github.com/taolong-thao/Socket-Client/blob/master/diagram-application.png?raw=true)
## Prerequisites

- JDK (Java Development Kit) 21 or later
- Maven
- Mysql
- Docker (Optional)
#### SQL Script
``src/main/resources/config/v1.sql
``
#### Docker compose file for database environment
``
docker-compose up
``
#### build maven for project
```bash
mvn clean install
```
### Server run
#### Prerequisites
- jdk 21 or later
```bash
java -cp Server-1.0-SNAPSHOT.jar Server
```

### Client run
#### Prerequisites
- jdk 21 or later
```bash
java -cp Client-1.0-SNAPSHOT.jar Client
```