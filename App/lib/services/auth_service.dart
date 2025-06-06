import 'dart:convert';

import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;

class AuthService {
  final String baseUrl = dotenv.env['API_BASE_URL'] ?? '';
  final storage = const FlutterSecureStorage();

  // OAuth 로그인 요청
  Future<Map<String, dynamic>> loginWithOAuth(String provider, String code) async {
    try {
      /// 이 부분 backend api 서버 주소로 바꾸면 됨 ///
      final response = await http.post(
        Uri.parse('$baseUrl/api/auth/$provider'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'code': code}),
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        // 토큰 저장
        await storage.write(key: 'access_token', value: data['access_token']);
        await storage.write(key: 'refresh_token', value: data['refresh_token']);
        return data;
      } else {
        throw Exception('로그인 실패');
      }
    } catch (e) {
      throw Exception('서버 통신 오류: $e');
    }
  }

  // 토큰 갱신
  Future<void> refreshToken() async {
    try {
      final refreshToken = await storage.read(key: 'refresh_token');
      final response = await http.post(
        Uri.parse('$baseUrl/api/auth/refresh'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'refresh_token': refreshToken}),
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        await storage.write(key: 'access_token', value: data['access_token']);
      } else {
        throw Exception('토큰 갱신 실패');
      }
    } catch (e) {
      throw Exception('토큰 갱신 오류: $e');
    }
  }

  // 로그아웃
  Future<void> logout() async {
    await storage.deleteAll();
  }
}
