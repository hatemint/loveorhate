# Love or Hate

## Setup
1. 프로젝트 루트에 위치한 `./setup.sh`를 실행해서 설치 및 실행을 완료합니다. (Docker가 설치되어 있어야합니다.)
```shell
$ ./setup.sh
```
2. `http` 디렉토리에 위치한 `*.http`를 실행하여 테스트를 수행합니다.

## Usages
### Infrastructure
- Redis: 키워드 검색 랭킹 구현을 위해 Redis를 사용하였습니다. 

### Dependencies
- Spring Cloud:
  - Open Feign: REST Client로 Open Feign을 사용하였습니다.
  - Circuit Breaker(Resilience4J): Open API 예외 발생시 Fallback 처리를 위해 Circuit Breaker를 사용하였습니다. 
- Jsoup: Naver Open API 검색시 검색결과의 HTML 태그 파싱을 위해 Jsoup 라이브러리를 사용하였습니다.


## Project Description

- RestTemplate vs Open Feign
  - HTTP Client를 사용하여 Open API 호출 기능을 구현하기 위해서 Retrofit, HttpComponents, RestTemplate 등의 여러가지 선택지가 있었습니다.
  - 서버에서 이미 스프링 사용하고 있고 비교적 손쉽게 구현할 수 있는 스프링에서 제공하는 라이브러리인 RestTemplate과 Open Feign 중에서 고민하게 되었습니다. 
  - 여러 리서치 결과 RestTemplate 대신에 Open Feign을 선택하게 되었는데 그 이뉴는 다음과 같습니다.
    1. RestTemplate의 경우 스프링 5버전대에서 지원하긴하지만 공식적으로 Deprecated될 것이라고 공지하였습니다.
    2. Open Feign의 RestTemplate에 비해서 인터페이스를 작성하고 어노테이션을 선언하기만 하면 되기 때문에 RestTemplate에 비해서 비교적 쉽게 클라이언트를 작성할 수 있다는 장점이 있습니다. (API 제공자가 확장되더라도 비교적 손쉽게 구성이 가능합니다)
    3. Spring Cloud와 Open Feign에서 제공하는 여러가지 기능들을 통해 손쉬운 Fallback 처리가 가능합니다. 
- 카카오 및 네이버 등의 검색 API 제공자의 장애 상황 발생시 장애 상황에 대한 로깅 및 일부 검색 API 제공자의 실패가 API의 실패로 이어지지 않도록 Fallback 처리하였습니다.
    - 한 개의 Open API 서비스를 이용하는 경우 제공자 서버의 예외가 발생할 때 특정 예외 상황에 대해 적절하게 처리하여 클라이언트에게 예외를 내려줄 수 있지만
    - 위의 요구사항과 같이 여러개의 Open API 서비스 요청이 하나의 API 응답을 이루고 있는 경우, 특정 제공자 서비스의 장애 상황이 API의 실패로 이어지면 안된다고 생각했습니다.
    - 때문에, 각각의 Open API 서비스 호출을 비동기 처리하였고, Open API의 connection 및 read timeout을 설정하여 특정 장애 상황에서 요청이 길게 블록되는 것을 방지하였습니다. (추후 서비스를 모니터링 해보고 해당 timeout 설정은 조정될 수 있습니다)
    - 예외 상황에 대해서 디버깅 및 트래킹 할 수 있도록 로그를 남기고 Fallback 처리하여 API가 정상적으로 수행될 수 있도록 구현하였습니다.
- 검색 API 제공자 서비스의 확장성을 고려한 설계
    - 검색 API 서비스 제공자가 추가되더라도 정의한 OpenApiService 인터페이스를 구현하기만하면 실제 조회를 수행하는 PlaceSearchService는 OpenApiService의 퍼블릭 인터페이스에만 의존하고 있기 때문에 영향을 주지 않고 확장할 수 있습니다.
    - 또한, 검색한 결과를 사용자가 정의한 기준대로 정렬하는 로직을 별도의 SearchResultSorter 인터페이스의 구현을 통해 관심사를 분리하였습니다.
- 키워드 별로 검색된 횟수 동시성 제어
  - 검색 키워드 랭킹 기능을 구현하기 몇가지 고민들이 있었고 결과적으로 Redis를 선택하여 키워드 랭킹 기능을 구현한 히스토리는 다음과 같습니다.
  - 가장 크게 고민한 부분은 키워드 조회 카운트 업데이트 동시성 문제와 키워드 검색 횟수의 실시간 정합성에 대한 부분이었습니다.
  - 가장 단순하게 키워드와 검색 횟수를 RDBMS에 관련한다고 가정했을 때, 조회수 카운트 업데이트는 다음의 과정을 거칩니다.
    1. 먼저 해당 키워드가 존재하는지 확인합니다. (모든 작업은 동일한 트랜잭션 안에서 수행됩니다)
    2-1. 해당 키워드가 존재하지 않는다면 해당 키워드를 조회수 카운트 1과 함께 저장합니다.
    2-2. 해당 키워드가 존재할 경우 해당 키워드를 정합성을 위해 `SELECT ... FOR UPDATE`를 사용해서 `X-LOCK`을 걸고 조회해옵니다.
    2. 해당 키워드의 카운트를 1증가시키고 다시 데이터베이스에 저장한 다음 트랜잭션을 종료합니다.
  - 위와 같이 구현할 경우 동시성 문제를 해결하기 위해서 `LOCK`을 걸고 데이터를 조회하고 업데이트하는 로직이 필요하며, 키워드 및 장소 검색 특성을 고려했을 때 일반적인 경우 전체 검색 서비스에 대한 트래픽은 많지만
    특정 키워드로 트래픽이 집중되지는 않을 것이라고 생각이들었고 이 경우 큰 문제가 발생하지 않는다고 생각했습니다.
  - 하지만, 이벤트가 발생하면 이야기가 달라지는데 현실 세계의 특정 이벤트 및 사건이 발생함에 따라서 특정 검색 키워드로 트래픽이 집중횔 수 있고 이 경우에는 위와 같은 로직으로 카운트를 처리하게 되면 성능상으로 이슈가 발생할 수 있겠다고 생각했습니다.
  - 방법 1. 키워드 검색 이벤트 로그
    - 특정 키워드를 검색했을 때마다 검색 이벤트 로그를 데이터베이스에 저장합니다. (ex. "who": "20대 남성", "keyword": "카카오뱅크", "createdAt": "2022-08-12 12:00")
    - 이후 키워드 별로 검색 횟수 랭킹을 집계할 때는 해당 데이터베이스에 키워드로 그룹화한 카운트 쿼리를 질의해서 상위 10개의 키워드를 추출합니다.
    - 이렇게 구성할 경우 장점은 특정 키워드 검색 자체는 누가 무엇을 언제 검색했는지가 추후 다른 BM을 리서치하거나 이벤트 로그를 분석하여 비즈니스적으로 유의미한 데이터를 얻어낼 수 있다고 생각이 되었고
    - 특정 키워드의 카운트에 대한 동시성 문제 및 잠금을 걸고 처리하는 등을 고민하지 않아도 된다는 점이 있습니다.
    - 다만, 위와 같은 방식의 문제점은 검색 이벤트 같은 경우는 검색 트래픽이 엄청 많기 때문에 실시간으로 엄청 방대한 데이터가 쌓일 것이고 RDBMS에 이것을 저장하고 조회해오는데는 한계가 있다고 생각이들었습니다.
    - 특히 위에서 수행할 쿼리가 문제인데, 실시간으로 얼마나 정확한 랭킹을 보여줄 것인지에 따라서 쿼리를 수행해야하는 빈도가 달라질 수 있고, 이 빈도가 잦으면 잦을수록 데이터베이스에 가해지는 부하가 커질 것으로 생각했습니다.
  - 방법 2. 레디스를 이용한 키워드 검색 카운트 관리
    - 키워드를 조회해서 로그로 남기는 것과 어느 기간동안 어떠한 키워드가 얼마나 많이 검색되었는지를 계산하는 것은 다른 관심사라고 생각이 들었고
    - 키워드 검색에 대한 카운트만 따로 관리할 수 있다면 위에서 고민하고 있는 부하를 줄일 수 있을것이라고 생각했습니다.
    - 몇가지 옵션들을 살펴보고나서 Redis의 SortedSet이라는 것을 이용해서 키워드 검색 카운트를 관리하면 쉽게 위에서 언급한 문제들을 해결할 수 있겠다고 판단이 들었습니다.
      1. Redis 같은 경우 싱글 스레드 이벤트 루프 기반으로 동작하기 때문에 동시성 문제로부터 자유롭습니다.
      2. Redis의 경우 다양한 자료구조를 지원하는데 SortedSet을 이용하면 손쉽게 데이터와 카운트를 관리하고 정렬할 수 있습니다.
      3. Spring Data Redis에서는 레디스에 대한 다양한 인터페이스를 지원하여 손쉽게 Redis의 여러가지 기능들을 활용할 수 있습니다.
    - 때문에 Redis를 이용해서 키워드를 검색하면 Redis의 ZINCRBY 명령어를 이용해서 특정 키워드 카운트를 증가시키고
    - 이후 랭킹을 조회할 때는 ZREVRANGE 명령어를 이용해서 정렬된 결과를 손쉽게 조회할 수 있었습니다.
    - 다만 현재 구현된 방식에서는 특정 기간동안 어떠한 키워드가 얼만큼 검색되었는지를 조회하는 것과 Expired Time이 설정되어있지 않은데 첫번째의 경우 SortedSet에서 관리하는 Key에 시간을 포함하여 적절히 나누고 ZUNION 오퍼레이션을 활용해서 해결이 가능할 것 같습니다.
      두번째의 경우는 실시간 키워드 검색 랭킹의 조회 기간이 설정된다면 적당한 Expired Time을 설정할 수 있을 것 같습니다.
    * 현재 구현에서는 키워드 검색시 로그를 RDBMS에 저장하는 로직을 구현되어있지 않습니다.
