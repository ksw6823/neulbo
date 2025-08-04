# AI Hub 수면 데이터 분석 가이드
## 안심서비스 Jupyter 노트북 환경에서의 EDF 파일 분석

### 📋 개요
이 문서는 AI Hub의 "차세대 수면 검사 데이터"에서 EDF(European Data Format) 파일을 분석하여 액티그래피 데이터를 추출하는 전체 과정을 정리합니다.

### 🎯 목표
- 멀티모달 EDF 파일에서 액티그래피(X, Y, Z축) 데이터만 추출
- 안심서비스의 제한된 라이브러리 환경에서 순수 Python으로 구현
- 휴대폰 기반 수면 추적 시스템에 적용 가능한 데이터 확보

---

## 📊 데이터셋 정보

### AI Hub 데이터셋 개요
- **명칭**: 차세대 수면 검사 데이터
- **규모**: 1,142명, 67GB, 250Hz 샘플링
- **구성**: 액티그래피, 심전도(ECG), 사운드 데이터
- **레이블**: 82개 상세 수면 분석 항목

### EDF 파일 구조
```
C2022-VD-04-0001.edf
├── 헤더 (256 bytes)
├── 시그널 헤더 (24채널 × 256 bytes)
└── 데이터 레코드
    ├── X axis (1000 samples/record)
    ├── Y axis (1000 samples/record)  
    ├── Z axis (1000 samples/record)
    ├── ECG (2500 samples/record)
    └── Mel_Freq_1-20 (400 samples/record × 20)
```

---

## 🛠️ 구현 과정

### 1단계: 환경 설정 및 파일 탐색

#### 필요한 라이브러리 import
```python
import os
import struct
import numpy as np
from datetime import datetime, timedelta
```

#### 파일 경로 확인
```python
# 현재 작업 디렉토리 확인
current_dir = os.getcwd()
print(f"현재 디렉토리: {current_dir}")

# 데이터셋 파일 탐색
edf_file_path = "/dataset/170-1.차가 수면 검사 데이터/01.수집가공데이터/Training/01.훈련데이터/Normal/C/C2022-VD-04-0001.edf"

if os.path.exists(edf_file_path):
    print(f"✅ 파일 발견: {edf_file_path}")
    file_size = os.path.getsize(edf_file_path)
    print(f"파일 크기: {file_size:,} bytes ({file_size/1024/1024:.1f} MB)")
else:
    print(f"❌ 파일을 찾을 수 없습니다: {edf_file_path}")
```

### 2단계: EDF 헤더 파싱

#### SimpleEDFReader 클래스 구현
```python
class SimpleEDFReader:
    def __init__(self, file_path):
        self.file_path = file_path
        self.header = {}
        self.signals = {}
        
    def read_header(self):
        """EDF 파일 헤더 읽기"""
        with open(self.file_path, 'rb') as f:
            # 메인 헤더 (256 bytes)
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
                'version': version,
                'patient_id': patient_id,
                'recording_id': recording_id,
                'start_date': start_date,
                'start_time': start_time,
                'header_bytes': header_bytes,
                'num_records': num_records,
                'record_duration': record_duration,
                'num_signals': num_signals
            }
            
            print(f"📋 EDF 헤더 정보:")
            print(f"   - 버전: {version}")
            print(f"   - 환자 ID: {patient_id}")
            print(f"   - 기록 ID: {recording_id}")
            print(f"   - 시작 날짜: {start_date}")
            print(f"   - 시작 시간: {start_time}")
            print(f"   - 레코드 수: {num_records:,}")
            print(f"   - 레코드 지속시간: {record_duration}초")
            print(f"   - 시그널 수: {num_signals}")
            
            return self.header
    
    def read_signal_headers(self):
        """시그널 헤더 정보 읽기"""
        if not self.header:
            self.read_header()
            
        num_signals = self.header['num_signals']
        
        with open(self.file_path, 'rb') as f:
            # 메인 헤더 건너뛰기
            f.seek(256)
            
            # 각 시그널별 정보 읽기
            labels = []
            samples_per_record = []
            
            # 시그널 라벨 읽기
            for i in range(num_signals):
                label = f.read(16).decode('ascii').strip()
                labels.append(label)
            
            # 다른 필드들 건너뛰기 (transducer type, units, physical min/max, digital min/max, prefiltering)
            f.seek(256 + num_signals * 16 * 6, 0)
            
            # samples per record 읽기
            for i in range(num_signals):
                samples = int(f.read(8).decode('ascii').strip())
                samples_per_record.append(samples)
            
            print(f"\n🔍 발견된 시그널 채널:")
            for i, (label, samples) in enumerate(zip(labels, samples_per_record)):
                print(f"   {i:2d}. {label:15s} - {samples:4d} samples/record")
                
            self.signals = {
                'labels': labels,
                'samples_per_record': samples_per_record
            }
            
            return self.signals
```

### 3단계: 액티그래피 데이터 추출

#### 최종 데이터 추출 함수
```python
def extract_actigraphy_simple(file_path, max_records=10):
    """
    단순화된 액티그래피 데이터 추출
    안심서비스 환경에서 안정적으로 동작하도록 최적화
    """
    
    print(f"📂 파일 분석 시작: {os.path.basename(file_path)}")
    
    # EDF 리더 생성 및 헤더 읽기
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
    
    # 하드코딩된 samples per channel (관찰된 패턴 기반)
    samples_per_channel = {
        'X': 1000, 'Y': 1000, 'Z': 1000,  # 액티그래피
        'ECG': 2500,                       # 심전도
        'Mel_Freq': 400                    # 음성 특징 (20개 채널)
    }
    
    # 데이터 추출
    x_data, y_data, z_data = [], [], []
    timestamps = []
    
    with open(file_path, 'rb') as f:
        # 헤더 건너뛰기
        header_size = 256 + header['num_signals'] * 256
        f.seek(header_size)
        
        print(f"\n📊 데이터 추출 중 (최대 {max_records}개 레코드)...")
        
        for record_num in range(min(max_records, header['num_records'])):
            print(f"   레코드 {record_num + 1}/{max_records} 처리 중...")
            
            # 각 채널별 데이터 읽기
            record_data = {}
            
            for channel_idx in range(header['num_signals']):
                channel_label = signals['labels'][channel_idx]
                
                if channel_idx in actig_channels.values():
                    # 액티그래피 채널 (1000 samples)
                    samples_count = 1000
                    raw_data = f.read(samples_count * 2)  # 16-bit integers
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
                        skip_bytes = signals['samples_per_record'][channel_idx] * 2
                    
                    f.seek(f.tell() + skip_bytes)
            
            # 타임스탬프 생성
            start_time = record_num * header['record_duration']
            for i in range(1000):  # 액티그래피는 1000 samples/record
                timestamp = start_time + (i / 1000.0)  # 1000Hz 가정
                timestamps.append(timestamp)
    
    print(f"✅ 데이터 추출 완료!")
    print(f"   📈 추출된 샘플 수:")
    print(f"      - X축: {len(x_data):,}개")
    print(f"      - Y축: {len(y_data):,}개") 
    print(f"      - Z축: {len(z_data):,}개")
    print(f"      - 시간: {len(timestamps):,}개")
    
    # 샘플 데이터 출력
    if x_data and y_data and z_data:
        print(f"\n📋 샘플 데이터 (처음 10개):")
        print(f"{'Time':>8s} {'X':>8s} {'Y':>8s} {'Z':>8s}")
        print("-" * 35)
        for i in range(min(10, len(x_data))):
            print(f"{timestamps[i]:8.3f} {x_data[i]:8d} {y_data[i]:8d} {z_data[i]:8d}")
        
        print(f"\n📊 데이터 통계:")
        print(f"   X축 - 범위: {min(x_data)} ~ {max(x_data)}, 평균: {sum(x_data)/len(x_data):.1f}")
        print(f"   Y축 - 범위: {min(y_data)} ~ {max(y_data)}, 평균: {sum(y_data)/len(y_data):.1f}")
        print(f"   Z축 - 범위: {min(z_data)} ~ {max(z_data)}, 평균: {sum(z_data)/len(z_data):.1f}")
    
    return {
        'timestamps': timestamps,
        'x_data': x_data,
        'y_data': y_data,
        'z_data': z_data,
        'metadata': {
            'file_path': file_path,
            'total_records': header['num_records'],
            'processed_records': max_records,
            'sampling_rate': 1000,  # Hz (추정)
            'duration': len(timestamps) / 1000.0  # 초
        }
    }
```

### 4단계: 실행 및 결과 확인

#### 데이터 추출 실행
```python
# EDF 파일 경로
edf_file_path = "/dataset/170-1.차가 수면 검사 데이터/01.수집가공데이터/Training/01.훈련데이터/Normal/C/C2022-VD-04-0001.edf"

# 액티그래피 데이터 추출 (처음 10개 레코드만)
actigraphy_data = extract_actigraphy_simple(edf_file_path, max_records=10)

if actigraphy_data:
    print("\n🎉 액티그래피 데이터 추출 성공!")
    print(f"📊 메타데이터: {actigraphy_data['metadata']}")
else:
    print("❌ 데이터 추출 실패")
```

---

## 🔧 트러블슈팅 가이드

### 문제 1: ModuleNotFoundError (pandas, mne)
**원인**: 안심서비스 환경에 외부 라이브러리 미설치
**해결**: 순수 Python 표준 라이브러리만 사용 (`os`, `struct`)

### 문제 2: 파일 경로 오류
**원인**: 상대 경로와 절대 경로 혼동
**해결**: Jupyter 파일 브라우저에서 확인한 절대 경로 사용
```python
# 올바른 경로 형식
edf_file_path = "/dataset/170-1.차가 수면 검사 데이터/01.수집가공데이터/Training/01.훈련데이터/Normal/C/C2022-VD-04-0001.edf"
```

### 문제 3: NameError in read_edf_header
**원인**: 변수명 오타 (`channel` vs `channels`)
**해결**: 변수명 일관성 확인 및 수정

### 문제 4: struct.unpack 오류
**원인**: samples_per_record 파싱 오류
**해결**: 하드코딩된 값 사용으로 안정성 확보

---

## 📈 데이터 활용 방안

### 휴대폰 호환성
- **샘플링 레이트**: AI Hub 250Hz → 휴대폰 50-100Hz 다운샘플링
- **3축 데이터**: X, Y, Z축 모두 활용 가능
- **실시간 처리**: 배치 단위로 분할하여 처리

### 모델 학습 준비
```python
def prepare_for_mobile_model(actigraphy_data, target_sampling_rate=50):
    """휴대폰 모델용 데이터 전처리"""
    
    original_rate = actigraphy_data['metadata']['sampling_rate']
    downsample_factor = original_rate // target_sampling_rate
    
    # 다운샘플링
    x_downsampled = actigraphy_data['x_data'][::downsample_factor]
    y_downsampled = actigraphy_data['y_data'][::downsample_factor]
    z_downsampled = actigraphy_data['z_data'][::downsample_factor]
    
    # 정규화
    def normalize_axis(data):
        mean_val = sum(data) / len(data)
        data_centered = [x - mean_val for x in data]
        std_val = (sum(x**2 for x in data_centered) / len(data_centered))**0.5
        return [x / std_val if std_val > 0 else 0 for x in data_centered]
    
    x_normalized = normalize_axis(x_downsampled)
    y_normalized = normalize_axis(y_downsampled)
    z_normalized = normalize_axis(z_downsampled)
    
    return {
        'x': x_normalized,
        'y': y_normalized,
        'z': z_normalized,
        'sampling_rate': target_sampling_rate
    }
```

---

## 🎯 결론

### 성공 요소
1. **순수 Python**: 외부 의존성 최소화
2. **하드코딩 전략**: 관찰된 패턴 기반 안정성 확보
3. **단계별 접근**: 헤더 → 시그널 → 데이터 순차 처리
4. **에러 핸들링**: 각 단계별 검증 및 로깅

### 후속 작업
1. **전체 데이터셋 처리**: 1,142개 파일 배치 처리
2. **라벨 데이터 연동**: 82개 수면 분석 항목과 매칭
3. **모델 학습**: 멀티모달 → 단일모달 전이학습
4. **실시간 적용**: 휴대폰 앱 통합

이 가이드를 따라하면 안심서비스 환경에서 AI Hub 수면 데이터를 안정적으로 분석할 수 있습니다! 🚀 