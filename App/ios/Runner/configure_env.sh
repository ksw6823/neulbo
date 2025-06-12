#!/bin/bash

# .env 파일 경로
ENV_FILE="$SRCROOT/../../.env"

# Info.plist 파일 경로
INFO_PLIST="$BUILT_PRODUCTS_DIR/$INFOPLIST_PATH"

if [ -f "$ENV_FILE" ]; then
    echo "Found .env file at $ENV_FILE"
    
    # 환경 변수 읽기
    export $(cat "$ENV_FILE" | grep -v '^#' | xargs)
    
    # Info.plist에서 플레이스홀더 교체
    if [ ! -z "$KAKAO_NATIVE_APP_KEY" ]; then
        /usr/libexec/PlistBuddy -c "Set CFBundleURLTypes:0:CFBundleURLSchemes:0 kakao$KAKAO_NATIVE_APP_KEY" "$INFO_PLIST"
        echo "Updated Kakao URL scheme: kakao$KAKAO_NATIVE_APP_KEY"
    fi
    
    if [ ! -z "$NAVER_CLIENT_ID" ]; then
        /usr/libexec/PlistBuddy -c "Set CFBundleURLTypes:0:CFBundleURLSchemes:1 naver$NAVER_CLIENT_ID" "$INFO_PLIST"
        echo "Updated Naver URL scheme: naver$NAVER_CLIENT_ID"
    fi
    
    echo "Environment variables configured successfully"
else
    echo "Warning: .env file not found at $ENV_FILE"
fi 