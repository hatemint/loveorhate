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
  - Open Feign: REST Client로 Open Feign을 사용하였습니다. ([RestTemplate vs WebClient vs Open Feign (feat. Open Feign을 선택한 이유)]())
  - Circuit Breaker(Resilience4J): Open API 예외 발생시 Fallback 처리를 위해 Circuit Breaker를 사용하였습니다. 
- Jsoup: Naver Open API 검색시 HTML 태그 파싱을 위해 Jsoup 라이브러리를 사용하였습니다.

