@echo off
echo 🚀 Neulbo Backend (AWS RDS) 배포 시작...

REM 환경변수 파일 확인
if not exist .env (
    echo ❌ .env 파일이 없습니다. env.example을 참고해서 .env 파일을 생성해주세요.
    pause
    exit /b 1
)

echo 🔨 Gradle 빌드 중...
call gradlew.bat clean bootJar

if %errorlevel% neq 0 (
    echo ❌ Gradle 빌드 실패!
    pause
    exit /b 1
)

echo 🐳 Docker Compose로 서비스 시작 중 (PostgreSQL 컨테이너 제외)...
docker-compose -f docker-compose.rds.yml down
docker-compose -f docker-compose.rds.yml up -d --build

echo ⏳ 서비스 시작 대기 중...
timeout /t 30 /nobreak > nul

echo 🔍 서비스 상태 확인 중...
docker-compose -f docker-compose.rds.yml ps

echo 🩺 헬스체크 실행 중...
for /l %%i in (1,1,5) do (
    curl -f http://localhost:8080/actuator/health > nul 2>&1
    if !errorlevel! equ 0 (
        echo ✅ API 서버가 정상적으로 실행되었습니다!
        echo.
        echo 📋 서비스 정보:
        echo    - API 서버: http://localhost:8080
        echo    - AWS RDS: 연결됨
        echo    - Redis: localhost:6379
        echo.
        echo 🔗 테스트 URL:
        echo    - 구글 로그인: POST http://localhost:8080/oauth/login/google
        echo    - 카카오 로그인: POST http://localhost:8080/oauth/login/kakao
        echo    - 네이버 로그인: POST http://localhost:8080/oauth/login/naver
        echo.
        echo 📊 로그 확인: docker-compose -f docker-compose.rds.yml logs -f api
        pause
        exit /b 0
    )
    echo    시도 %%i/5: API 서버 응답 대기 중...
    timeout /t 10 /nobreak > nul
)

echo ❌ API 서버가 시작되지 않았습니다. 로그를 확인해주세요:
echo    docker-compose -f docker-compose.rds.yml logs api
pause
exit /b 1 