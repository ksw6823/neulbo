import 'package:flutter/material.dart';
import 'package:flutter_appauth/flutter_appauth.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:kakao_flutter_sdk/kakao_flutter_sdk.dart';
import 'dart:io' show Platform;

import '../services/auth_service.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final FlutterAppAuth _appAuth = const FlutterAppAuth();
  final AuthService _authService = AuthService();

  /// 카카오 로그인 버튼 클릭 시 호출
  Future<void> _loginWithKakao() async {
    try {
      final code = await _getKakaoAuthCode();
      if (code != null) {
        final response = await _authService.loginWithOAuth('kakao', code);
        print('✅ Kakao 로그인 성공: ${response['access_token']}');
      } else {
        print('❌ 인가 코드가 null입니다.');
      }
    } catch (e) {
      print('❌ 카카오 로그인 전체 실패: $e');
    }
  }

  /// 인가 코드 요청 (카카오톡 우선, 없으면 계정 로그인)
  Future<String?> _getKakaoAuthCode() async {
    final redirectUri = 'kakao${dotenv.env['KAKAO_NATIVE_APP_KEY']}://oauth';

    try {
      if (await isKakaoTalkInstalled()) {
        // 카카오톡으로 로그인
        return await AuthCodeClient.instance.authorizeWithTalk(redirectUri: redirectUri);
      } else {
        // 카카오 계정 로그인
        return await AuthCodeClient.instance.authorize(redirectUri: redirectUri);
      }
    } catch (e) {
      print('⚠️ 인가 코드 요청 실패: $e');
      return null;
    }
  }

  /// 네이버 로그인 버튼 클릭 시 호출
  Future<void> _loginWithNaver() async {
    try {
      final AuthorizationResponse? result = await _appAuth.authorize(
        AuthorizationRequest(
          dotenv.env['NAVER_CLIENT_ID'] ?? '',
          'naver${dotenv.env['NAVER_CLIENT_ID']}://oauth',
          serviceConfiguration: const AuthorizationServiceConfiguration(
            authorizationEndpoint: 'https://nid.naver.com/oauth2.0/authorize',
            tokenEndpoint: 'https://nid.naver.com/oauth2.0/token',
          ),
          scopes: ['profile', 'email'],
        ),
      );

      if (result != null && result.authorizationCode != null) {
        // 인가 코드를 백엔드로 전달
        final response = await _authService.loginWithOAuth('naver', result.authorizationCode!);
        print('✅ Naver 로그인 성공: ${response['access_token']}');
      } else {
        print('❌ 네이버 인가 코드가 null입니다.');
      }
    } catch (e) {
      print('❌ 네이버 로그인 실패: $e');
    }
  }

  /// 구글 로그인 버튼 클릭 시 호출
  Future<void> _loginWithGoogle() async {
    try {
      // 플랫폼별로 다른 Client ID 사용
      String clientId;
      String redirectUri;
      
      if (Platform.isAndroid) {
        clientId = dotenv.env['GOOGLE_CLIENT_ID_ANDROID'] ?? '';
        redirectUri = 'com.googleusercontent.apps.${clientId.split('.').reversed.join('.')}://oauth';
      } else if (Platform.isIOS) {
        clientId = dotenv.env['GOOGLE_CLIENT_ID_IOS'] ?? '';
        redirectUri = 'com.googleusercontent.apps.${clientId.split('.').reversed.join('.')}://oauth';
      } else {
        print('❌ 지원하지 않는 플랫폼입니다.');
        return;
      }

      final AuthorizationResponse? result = await _appAuth.authorize(
        AuthorizationRequest(
          clientId,
          redirectUri,
          serviceConfiguration: const AuthorizationServiceConfiguration(
            authorizationEndpoint: 'https://accounts.google.com/o/oauth2/auth',
            tokenEndpoint: 'https://oauth2.googleapis.com/token',
          ),
          scopes: ['email', 'profile'],
        ),
      );

      if (result != null && result.authorizationCode != null) {
        final response = await _authService.loginWithOAuth('google', result.authorizationCode!);
        print('✅ Google 로그인 성공: ${response['access_token']}');
      } else {
        print('❌ 구글 인가 코드가 null입니다.');
      }
    } catch (e) {
      print('❌ 구글 로그인 실패: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.grey,
      body: SafeArea(
        child: Center(
          child: Column(
            children: [
              SizedBox(
                height: 200,
              ),
              Text(
                "쿨쿨",
                style: TextStyle(
                    fontWeight: FontWeight.w700,
                    fontSize: 100,
                    color: Colors.white,
                    letterSpacing: -5,
                    shadows: [
                      Shadow(
                          color: Colors.black.withOpacity(0.3),
                          offset: const Offset(3, 3),
                          blurRadius: 15),
                    ]),
              ),
              SizedBox(height: 200),
              ElevatedButton(
                onPressed: _loginWithKakao,
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFFFEE500), // 카카오 노란색
                  foregroundColor: Colors.black87,
                  minimumSize: const Size(200, 50),
                ),
                child: const Text('카카오로 로그인'),
              ),
              const SizedBox(height: 16),
              ElevatedButton(
                onPressed: _loginWithNaver,
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFF03C75A), // 네이버 초록색
                  foregroundColor: Colors.white,
                  minimumSize: const Size(200, 50),
                ),
                child: const Text('네이버로 로그인'),
              ),
              const SizedBox(height: 16),
              ElevatedButton(
                onPressed: _loginWithGoogle,
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.white,
                  foregroundColor: Colors.black87,
                  minimumSize: const Size(200, 50),
                  side: const BorderSide(color: Colors.grey),
                ),
                child: const Text('Google로 로그인'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
