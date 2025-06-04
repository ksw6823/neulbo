import 'package:flutter/material.dart';
import 'package:flutter_appauth/flutter_appauth.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

import '../services/auth_service.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final FlutterAppAuth _appAuth = const FlutterAppAuth();
  final AuthService _authService = AuthService();

  Future<void> _loginWithKakao() async {
    try {
      final AuthorizationTokenResponse? result = await _appAuth.authorizeAndExchangeCode(
        AuthorizationTokenRequest(
          dotenv.env['KAKAO_CLIENT_ID'] ?? '',
          'kakao${dotenv.env['KAKAO_NATIVE_APP_KEY']}://oauth',
          serviceConfiguration: const AuthorizationServiceConfiguration(
            authorizationEndpoint: 'https://kauth.kakao.com/oauth/authorize',
            tokenEndpoint: 'https://kauth.kakao.com/oauth/token',
          ),
          scopes: ['profile_nickname', 'account_email'],
        ),
      );

      if (result != null) {
        final response = await _authService.loginWithOAuth('kakao', result.authorizationCode!);
        // 로그인 성공 처리
        print('Kakao 로그인 성공: ${response['access_token']}');
      }
    } catch (e) {
      print('Kakao 로그인 실패: $e');
    }
  }

  Future<void> _loginWithNaver() async {
    try {
      final AuthorizationTokenResponse? result = await _appAuth.authorizeAndExchangeCode(
        AuthorizationTokenRequest(
          dotenv.env['NAVER_CLIENT_ID'] ?? '',
          'naver${dotenv.env['NAVER_CLIENT_ID']}://oauth',
          serviceConfiguration: const AuthorizationServiceConfiguration(
            authorizationEndpoint: 'https://nid.naver.com/oauth2.0/authorize',
            tokenEndpoint: 'https://nid.naver.com/oauth2.0/token',
          ),
          scopes: ['profile', 'email'],
        ),
      );

      if (result != null) {
        final response = await _authService.loginWithOAuth('naver', result.authorizationCode!);
        // // 로그인 성공 처리
        print('Naver 로그인 성공: ${response['access_token']}');
      }
    } catch (e) {
      print('Naver 로그인 실패: $e');
    }
  }

  Future<void> _loginWithGoogle() async {
    try {
      final AuthorizationTokenResponse? result = await _appAuth.authorizeAndExchangeCode(
        AuthorizationTokenRequest(
          dotenv.env['GOOGLE_CLIENT_ID'] ?? '',
          'com.googleusercontent.apps.${dotenv.env['GOOGLE_CLIENT_ID']}://oauth',
          serviceConfiguration: const AuthorizationServiceConfiguration(
            authorizationEndpoint: 'https://accounts.google.com/o/oauth2/auth',
            tokenEndpoint: 'https://oauth2.googleapis.com/token',
          ),
          scopes: ['email', 'profile'],
        ),
      );

      if (result != null) {
        final response = await _authService.loginWithOAuth('google', result.authorizationCode!);
        // 로그인 성공 처리
        print('Google 로그인 성공: ${response['access_token']}');
      }
    } catch (e) {
      print('Google 로그인 실패: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.grey,
      body: SafeArea(
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
    );
  }
}
