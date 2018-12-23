﻿# HTTP1.0-Server

#### 프로그램 개요
컴퓨터네트워크 강의를 들으며 개발한 HTTP/1.0 스펙을 지원하는 서버입니다.
RFC1945 문서를 읽으며 구현했기 때문에,
표준 문서에 있는 그대로 구현하는 것을 기본 목표로 하였습니다.
개발 기간이 10일밖에 되지 않았기 때문에 Desktop환경의 Firefox와 Chrome에서만 작동을 확인했습니다. 

**Java의 HTTP지원을 위한 라이브러리들을 전혀 이용하지 않음**

#### 지원하는 기능 및 특장점
- HTTP/1.0과 HTTP/0.9 메시지를 읽을 수 있음
- 문서에 명시된 세 가지 시간 형식(ASCTIME, RFC1036, RFC1123)을 전부 읽을 수 있음
- www-authentication을 BASIC 포맷으로 지원함
- RequestServlet DTO, ResponseServlet DTO 객체들을 이용해 원하는 메시지를 생성하거나 받을 수 있음

#### 지원하지 않는 기능들
- RFC1945 문서의 11장(Access Authentication)과 부록 D는 전혀 구현되지 않음


당시 과제가 기본적인 라이브러리들만 이용해 어떠한 프로토콜 스펙을 구현하는 것이 목표였기에,
조금 부족한 부분들이 있을 수 있을것 같습니다. 그러나 HTTP/1.0프로토콜의 메시지 해석과 생성을 배우고자 하시는 분 들은 참고하시기에 적합할 것 같습니다.
