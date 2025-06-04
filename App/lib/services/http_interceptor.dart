import 'package:http/http.dart' as http;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'auth_service.dart';

class HttpInterceptor {
  final AuthService _authService = AuthService();
  final storage = const FlutterSecureStorage();

  Future<http.Response> interceptRequest(http.Request request) async {
    final accessToken = await storage.read(key: 'access_token');
    if (accessToken != null) {
      request.headers['Authorization'] = 'Bearer $accessToken';
    }
    return http.Response.fromStream(await request.send());
  }

  Future<http.Response> handleResponse(http.Response response) async {
    if (response.statusCode == 401) {
      // 토큰 만료 시 갱신
      await _authService.refreshToken();
      final newAccessToken = await storage.read(key: 'access_token');
      
      // 원래 요청 재시도
      final originalRequest = response.request!;
      originalRequest.headers['Authorization'] = 'Bearer $newAccessToken';
      return http.Response.fromStream(await originalRequest.send());
    }
    return response;
  }
} 