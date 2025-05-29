🎫 CamTicket - 캠퍼스 공연 예매 시스템
한동대학교 캠퍼스 내 공연 및 이벤트 예매를 위한 백엔드 API 시스템

📋 프로젝트 개요
CamTicket은 대학 캠퍼스 내에서 진행되는 다양한 공연(뮤지컬, 연극, 콘서트 등)의 예매를 관리하는 시스템입니다. 동아리 관리자가 공연을 등록하고, 학생들이 편리하게 예매할 수 있는 플랫폼을 제공합니다.

🚀 주요 기능
🎭 공연 관리
공연 등록: 동아리 관리자의 공연 게시글 작성 및 관리
회차 관리: 다중 공연 회차 설정 및 일정 관리
좌석 관리: 지정석/자유석 방식 지원
이미지 관리: AWS S3 연동 프로필 및 상세 이미지 업로드
🎟️ 예매 시스템
다중 티켓 옵션: 일반석, 새내기석 등 여러 옵션 동시 주문 지원
좌석 선택: 실시간 좌석 상태 확인 및 선택
예매 상태 관리: PENDING → APPROVED → REFUNDED 상태 플로우
계좌 정보 관리: 입금 확인을 위한 사용자 계좌 정보 수집
👥 사용자 관리
카카오 로그인: OAuth2 기반 간편 로그인
권한 관리: 일반 사용자 / 동아리 관리자 / 시스템 관리자
프로필 관리: 닉네임, 소개글, 프로필 이미지 수정
📊 관리자 기능
예매 현황 조회: 공연별 예매 신청 목록 확인
예매 승인/거절: 입금 확인 후 예매 상태 변경
환불 처리: 환불 신청 승인/거절 관리
🛠️ 기술 스택
Backend
언어: Kotlin
프레임워크: Spring Boot 3.4.4
ORM: Spring Data JPA + Hibernate
보안: Spring Security + JWT
데이터베이스: MySQL 8.0
Infrastructure
클라우드: AWS
파일 저장: AWS S3
문서화: Swagger (OpenAPI 3.0)
빌드 도구: Gradle
Development
JDK: OpenJDK 17
IDE: IntelliJ IDEA
버전 관리: Git & GitHub
📁 프로젝트 구조
src/main/kotlin/org/example/camticketkotlin/
├── config/              # 설정 파일들
│   ├── SecurityConfig.kt
│   ├── SwaggerConfig.kt
│   └── CorsProperties.kt
├── controller/          # REST API 컨트롤러
│   ├── AuthController.kt
│   ├── ReservationController.kt
│   └── PerformanceManagementController.kt
├── domain/             # 엔티티 클래스들
│   ├── User.kt
│   ├── PerformancePost.kt
│   ├── ReservationRequest.kt
│   └── enums/
├── dto/                # 데이터 전송 객체들
│   ├── request/
│   └── response/
├── service/            # 비즈니스 로직
│   ├── AuthService.kt
│   ├── ReservationService.kt
│   └── PerformanceManagementService.kt
├── repository/         # 데이터 접근 계층
└── util/               # 유틸리티 클래스들
    └── JwtUtil.kt
🚀 시작하기
필수 조건
JDK 17 이상
MySQL 8.0
Gradle 8.x
설치 및 실행
프로젝트 클론
bash
git clone https://github.com/xlxhollywood/camticket-back-kotlin.git
cd camticket-back-kotlin
데이터베이스 설정
sql
CREATE DATABASE camticket;
CREATE USER 'camticket_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON camticket.* TO 'camticket_user'@'localhost';
환경 변수 설정
bash
# application.yml 파일 생성 (gitignore에 포함됨)
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/camticket
    username: camticket_user
    password: your_password
  
  cloud:
    aws:
      s3:
        bucket: your-s3-bucket
      region:
        static: ap-northeast-2
      credentials:
        access-key: your-access-key
        secret-key: your-secret-key

custom:
  jwt:
    secret: your-jwt-secret-key
    expire-time-ms: 3600000
    
kakao:
  api:
    key:
      client: your-kakao-client-key
애플리케이션 실행
bash
./gradlew bootRun
API 문서 확인
http://localhost:8080/swagger-ui.html
📚 API 문서
주요 엔드포인트
🔐 인증
GET /camticket/auth/kakao-login - 카카오 로그인
🎭 공연 관리
GET /camticket/api/performance/overview - 전체 공연 목록
POST /camticket/api/performance-management - 공연 등록
GET /camticket/api/performance-management/{postId} - 공연 상세 조회
🎟️ 예매
GET /camticket/api/reservation/schedules/{postId} - 공연 회차 목록
GET /camticket/api/reservation/seats/{scheduleId} - 좌석 정보
POST /camticket/api/reservation - 예매 신청
GET /camticket/api/reservation/{reservationId}/detail - 예매 상세 조회
👤 사용자
PATCH /camticket/api/user/profile - 프로필 수정
GET /camticket/api/user/managers - 관리자 목록
자세한 API 명세는 Swagger 문서를 참고하세요.

💾 데이터베이스 스키마
주요 테이블
users - 사용자 정보
performance_post - 공연 게시글
performance_schedule - 공연 회차
reservation_request - 예매 신청
ticket_option - 티켓 옵션 (일반석, 새내기석 등)
schedule_seat - 좌석 정보
🔧 개발 환경 설정
IDE 설정 (IntelliJ IDEA)
Kotlin 플러그인 활성화
Spring Boot 플러그인 설치
코드 스타일: Google Java Style Guide
코딩 컨벤션
언어: Kotlin 코드 스타일 가이드 준수
커밋 메시지: Google Conventional Commits (한글 버전)
API 응답: RESTful API 설계 원칙 준수
🤝 기여하기
Fork the Project
Create your Feature Branch (git checkout -b feature/AmazingFeature)
Commit your Changes (git commit -m 'feat: Add some AmazingFeature')
Push to the Branch (git push origin feature/AmazingFeature)
Open a Pull Request
📝 라이센스
이 프로젝트는 MIT 라이센스 하에 배포됩니다. 자세한 내용은 LICENSE 파일을 참고하세요.

📞 연락처
프로젝트 관련 문의사항이 있으시면 언제든 연락주세요!

CamTicket - 더 나은 캠퍼스 공연 문화를 위하여 🎭✨

