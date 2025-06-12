# Flutter 프로젝트 빌드 자동화 Makefile

.PHONY: setup-ios setup-android build-ios build-android clean

# iOS 환경 설정
setup-ios:
	@echo "Setting up iOS environment..."
	cd ios/Runner && dart configure_ios_env.dart

# Android는 이미 Gradle에서 자동 처리됨
setup-android:
	@echo "Android environment is automatically configured by Gradle"

# iOS 빌드
build-ios: setup-ios
	@echo "Building for iOS..."
	flutter build ios

# Android 빌드
build-android:
	@echo "Building for Android..."
	flutter build apk

# 클린
clean:
	flutter clean

# 패키지 설치
deps:
	flutter pub get 