# AI Hub 수면 데이터 분석 - 빠른 참조 가이드

## 🚀 즉시 실행 코드

### 1단계: 환경 설정
```python
import os
import struct
```

### 2단계: 파일 경로 설정
```python
edf_file_path = "/dataset/170-1.차가 수면 검사 데이터/01.수집가공데이터/Training/01.훈련데이터/Normal/C/C2022-VD-04-0001.edf"
```

### 3단계: 완전한 실행 코드
```python
class SimpleEDFReader:
    def __init__(self, file_path):
        self.file_path = file_path
        self.header = {}
        self.signals = {}
        
    def read_header(self):
        with open(self.file_path, 'rb') as f:
            version = f.read(8).decode('ascii').strip()
            patient_id = f.read(80).decode('ascii').strip()
            recording_id = f.read(80).decode('ascii').strip()
            start_date = f.read(8).decode('ascii').strip()
            start_time = f.read(8).decode('ascii').strip()
            header_bytes = int(f.read(8).decode('ascii').strip())
            reserved = f.read(44).decode('ascii').strip()
            num_records = int(f.read(8).decode('ascii').strip())
            record_duration = float(f.read(8).decode('ascii').strip())
            num_signals = int(f.read(4).decode('ascii').strip())
            
            self.header = {
                'num_records': num_records,
                'record_duration': record_duration,
                'num_signals': num_signals
            }
            return self.header
    
    def read_signal_headers(self):
        if not self.header:
            self.read_header()
            
        num_signals = self.header['num_signals']
        
        with open(self.file_path, 'rb') as f:
            f.seek(256)
            
            labels = []
            for i in range(num_signals):
                label = f.read(16).decode('ascii').strip()
                labels.append(label)
            
            self.signals = {'labels': labels}
            return self.signals

def extract_actigraphy_simple(file_path, max_records=10):
    reader = SimpleEDFReader(file_path)
    header = reader.read_header()
    signals = reader.read_signal_headers()
    
    # 액티그래피 채널 찾기
    actig_channels = {}
    for i, label in enumerate(signals['labels']):
        if 'X axis' in label:
            actig_channels['X'] = i
        elif 'Y axis' in label:
            actig_channels['Y'] = i  
        elif 'Z axis' in label:
            actig_channels['Z'] = i
    
    if not actig_channels:
        print("❌ 액티그래피 채널을 찾을 수 없습니다!")
        return None
        
    print(f"✅ 액티그래피 채널 발견: {actig_channels}")
    
    # 데이터 추출
    x_data, y_data, z_data = [], [], []
    timestamps = []
    
    with open(file_path, 'rb') as f:
        header_size = 256 + header['num_signals'] * 256
        f.seek(header_size)
        
        for record_num in range(min(max_records, header['num_records'])):
            for channel_idx in range(header['num_signals']):
                channel_label = signals['labels'][channel_idx]
                
                if channel_idx in actig_channels.values():
                    # 액티그래피 채널 (1000 samples)
                    samples_count = 1000
                    raw_data = f.read(samples_count * 2)
                    values = struct.unpack(f'<{samples_count}h', raw_data)
                    
                    if channel_idx == actig_channels['X']:
                        x_data.extend(values)
                    elif channel_idx == actig_channels['Y']:
                        y_data.extend(values)
                    elif channel_idx == actig_channels['Z']:
                        z_data.extend(values)
                else:
                    # 다른 채널들 건너뛰기
                    if 'ECG' in channel_label:
                        skip_bytes = 2500 * 2
                    elif 'Mel_Freq' in channel_label:
                        skip_bytes = 400 * 2
                    else:
                        skip_bytes = 1000 * 2  # 기본값
                    
                    f.seek(f.tell() + skip_bytes)
            
            # 타임스탬프 생성
            start_time = record_num * header['record_duration']
            for i in range(1000):
                timestamp = start_time + (i / 1000.0)
                timestamps.append(timestamp)
    
    print(f"✅ 데이터 추출 완료!")
    print(f"   - X축: {len(x_data):,}개")
    print(f"   - Y축: {len(y_data):,}개") 
    print(f"   - Z축: {len(z_data):,}개")
    
    return {
        'timestamps': timestamps,
        'x_data': x_data,
        'y_data': y_data,
        'z_data': z_data
    }

# 실행
actigraphy_data = extract_actigraphy_simple(edf_file_path, max_records=10)

if actigraphy_data:
    print("🎉 성공!")
    # 샘플 데이터 출력
    for i in range(min(10, len(actigraphy_data['x_data']))):
        print(f"Time: {actigraphy_data['timestamps'][i]:.3f}s, "
              f"X: {actigraphy_data['x_data'][i]}, "
              f"Y: {actigraphy_data['y_data'][i]}, "
              f"Z: {actigraphy_data['z_data'][i]}")
```

---

## 🔧 주요 트러블슈팅

### 문제: ModuleNotFoundError
**해결**: `import pandas` 등 제거, 순수 Python만 사용

### 문제: 파일 경로 오류
**해결**: Jupyter 파일 브라우저에서 절대 경로 복사

### 문제: NameError
**해결**: 변수명 오타 확인 (`channel` → `channels`)

---

## 📊 데이터 구조

### EDF 파일 채널 구성
- **X axis**: 1000 samples/record (액티그래피)
- **Y axis**: 1000 samples/record (액티그래피)  
- **Z axis**: 1000 samples/record (액티그래피)
- **ECG**: 2500 samples/record (심전도)
- **Mel_Freq_1-20**: 400 samples/record × 20 (음성)

### 추출 결과
```
시간: 초 단위 (0.000, 0.001, 0.002, ...)
X/Y/Z: 16비트 정수 값 (-32768 ~ 32767)
샘플링: 1000Hz (추정)
```

---

## 🎯 핵심 포인트

1. **순수 Python**: 외부 라이브러리 없이 `os`, `struct`만 사용
2. **하드코딩**: samples_per_record 파싱 대신 관찰된 값 사용
3. **단계적 접근**: 헤더 → 시그널 → 데이터 순서로 처리
4. **에러 방지**: 각 단계별 검증 및 명확한 에러 메시지

이 코드를 복사해서 Jupyter 노트북에 붙여넣기만 하면 바로 실행됩니다! 🚀 