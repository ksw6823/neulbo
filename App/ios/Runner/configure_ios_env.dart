import 'dart:io';

void main() {
  final envFile = File('../../.env');
  final infoPlistFile = File('Info.plist');
  
  if (!envFile.existsSync()) {
    print('Warning: .env file not found');
    return;
  }
  
  if (!infoPlistFile.existsSync()) {
    print('Warning: Info.plist file not found');
    return;
  }
  
  // .env 파일 읽기
  final envContent = envFile.readAsStringSync();
  final envMap = <String, String>{};
  
  for (final line in envContent.split('\n')) {
    if (line.trim().isNotEmpty && line.contains('=')) {
      final parts = line.split('=');
      if (parts.length >= 2) {
        final key = parts[0].trim();
        final value = parts.sublist(1).join('=').trim();
        envMap[key] = value;
      }
    }
  }
  
  // Info.plist 읽기
  var infoPlistContent = infoPlistFile.readAsStringSync();
  
  // URL 스킴 교체
  final kakaoKey = envMap['KAKAO_NATIVE_APP_KEY'];
  final naverClientId = envMap['NAVER_CLIENT_ID'];
  
  if (kakaoKey != null) {
    infoPlistContent = infoPlistContent.replaceAll(
      'kakao_placeholder',
      'kakao$kakaoKey'
    );
    print('Updated Kakao URL scheme: kakao$kakaoKey');
  }
  
  if (naverClientId != null) {
    infoPlistContent = infoPlistContent.replaceAll(
      'naver_placeholder',
      'naver$naverClientId'
    );
    print('Updated Naver URL scheme: naver$naverClientId');
  }
  
  // Info.plist 파일 쓰기
  infoPlistFile.writeAsStringSync(infoPlistContent);
  print('iOS environment configuration completed');
} 