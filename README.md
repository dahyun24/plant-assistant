# 🌱 Multimodal RAG for Plant Assistant (Text + Image + Sensor)

## 1. Motivation
식물 관리 및 진단에는 **텍스트 정보**, **이미지(잎 사진 등)**, **환경 센서 데이터**가 동시에 중요합니다.  
하지만 기존 RAG(Retrieval-Augmented Generation) 시스템은 주로 텍스트에만 집중하여  
멀티모달 데이터를 충분히 활용하지 못한다는 한계가 있습니다.  

본 프로젝트는 **텍스트 + 이미지 + 센서 데이터를 동시에 처리할 수 있는 멀티모달 RAG 챗봇**을 구현하는 것을 목표로 합니다.  

---

## 2. Related Works
본 연구 설계는 최신 연구들을 참고하여 이루어졌습니다.

- [**Google DeepMind 연구: On the Theoretical Limitations of Embedding-Based Retrieval**](https://arxiv.org/pdf/2508.21038)
  - 단일 벡터(single vector) 표현은 retrieval 과정에서 중요한 정보를 잃게 됨을 지적  
  - 문서를 **여러 벡터(multi-vector)**로 표현하는 Retrieval 방식을 제안  

- [**Beyond Text: Optimizing RAG with Multimodal Inputs for Industrial Applications**](https://arxiv.org/abs/2410.21943)
  - 이미지 처리 전략 비교  
    - (A) CLIP 기반 멀티모달 임베딩  
    - (B) 이미지 요약 → 텍스트 임베딩 (**효과적**)  
  - **Combined Vector Store**: 텍스트와 이미지 요약을 같은 공간에 저장하는 전략 제안  

- [**AWS Tech Blog – Amazon Bedrock과 OpenSearch를 활용한 Multimodal RAG 기반 상품 검색 챗봇**](https://aws.amazon.com/ko/blogs/tech/bedrock-multimodal-rag-chatbot/)
  - 실제 산업 적용 사례  
  - 문서 하나에 텍스트, 이미지, 요약, 센서 데이터를 **멀티벡터**로 저장  
  - Fusion 및 Reranker로 retrieval 품질 개선

---

## 3. Our Approach

### (1) Embedding 단계
- **텍스트 정보** → Huggingface Embedding model
- **이미지 데이터** → Gemini API로 캡션 생성 후 텍스트 임베딩
- **센서 데이터** → 센서값 그대로 벡터화  

### (2) 저장소 구조
- **Combined Vector Store**: 텍스트 + 이미지 요약을 같은 공간에 저장  
- **Multi-Vector Store**: 문서 단위로 Combined Store 임베딩과 센서 벡터를 연결  

### (3) Retrieval 전략
- 기본적으로 **Multi-Vector Retrieval**을 채택  
- 질의 유형에 따라 modality별 가중치 조정  
  - 예: 환경 관련 → 센서 벡터 강화  
  - 예: 증상 관련 → 이미지 요약 강화  

### (4) Fusion
- 초기: 단순 **score aggregation (가중치 합산)**  
- 확장: **Late Fusion** 또는 **Cross-Encoder reranking**  

---

## 4. System Architecture
```text
사용자 질의 (텍스트/이미지/센서)
   ↓
[Embedding 단계]
   - 텍스트: hugging face model
   - 이미지: 캡션 생성 → 텍스트 임베딩
   - 센서: 센서값 그대로 벡터화
   ↓
[저장 단계]
   - Combined Vector Store (텍스트 + 이미지 요약)
   - Multi-Vector Store (문서별 여러 벡터 연결, 센서 포함)
   ↓
[Retrieval 단계]
   - Multi-Vector Retrieval
   - Query-Adaptive Fusion
   ↓
[LLM 응답 생성]
   - Retrieval evidence를 prompt에 삽입
   - LLM이 최종 답변 생성
```

## 5. Expected Outcome

- 텍스트, 이미지, 센서 데이터를 모두 활용한 정밀한 식물 상태 진단
- 단일 벡터 기반(CLIP 등) 대비 정보 손실 감소
- 멀티모달 데이터를 활용하는 RAG의 실제 구현 사례 제공

---

## 6. Dataset Description

### 📦 Dataset Name

**원예식물 화분류 물주기·수분공급 주기 생육데이터**
(Data on horticultural plants growth according to watering cycle)
<br>
출처: [AI Hub (원예식물(화분류) 물주기(수분공급 주기) 생육 데이터)](https://www.aihub.or.kr/aihubdata/data/view.do?pageIndex=1&currMenu=115&topMenu=100&srchOptnCnd=OPTNCND001&searchKeyword=&srchDetailCnd=DETAILCND001&srchOrder=ORDER001&srchPagePer=20&srchDataRealmCode=REALM004&aihubDataSe=data&dataSetSn=71705)

### 🧩 Overview

이 데이터셋은 **일반 가정 환경(거실, 베란다 등)** 에서 다양한 물주기 조건(건조 / 일반 / 과습) 하에
15종의 원예식물을 대상으로 촬영한 **이미지 + 센서 + 메타데이터**로 구성되어 있습니다.
스마트 식물재배 및 물주기 최적화 시스템 개발을 위해 구축된 학습용 데이터입니다.

### 🌿 Composition

| 구성 요소   | 설명                            | 형식                           | 개수       |
| ------- | ----------------------------- | ---------------------------- | -------- |
| 이미지 데이터 | 식물의 잎·줄기·뿌리 사진                | `.jpg`                       | 495,359장 |
| 센서 데이터  | 온도, 습도, CO₂, 일사량, 토양상/하단 환경 등 | `.json`                      | 495,359건 |
| 폴리곤 라벨  | 잎 부위(상엽, 하엽, 좌엽, 우엽)          | `annotation[].plant_polygon` | 각 이미지별   |
| 메타정보    | 장소, 식물명, 생육단계, 환경(건조/습한흙 등)   | JSON key-value               | 포함       |

### 🪴 Plant Species (15종)

몬스테라, 보스턴고사리, 홍콩야자, 스파티필럼, 테이블야자, 호접란, 부레옥잠,
선인장, 스투키, 금전수, 벵갈고무나무, 디펜바키아, 관음죽, 오렌지자스민, 올리브나무.

### 🌡 Sensor Fields Example

각 식물 JSON에는 다음과 같은 환경변수가 포함됩니다:

| 변수명                       | 설명             | 단위              |
| ------------------------- | -------------- | --------------- |
| `AirTemperature`          | 대기 온도          | °C              |
| `AirHumidity`             | 대기 습도          | %               |
| `Co2`                     | CO₂ 농도         | ppm             |
| `Quantum`                 | 일사량            | μmol/m²/s       |
| `SupplyEC`, `SupplyPH`    | 급수의 전기전도도, pH  | dS/m, pH        |
| `HighSoilTemp/Humi/EC/PH` | 흙 상단 온도·습도·영양분 | °C, %, dS/m, pH |
| `LowSoilTemp/Humi/EC/PH`  | 흙 하단 온도·습도·영양분 | °C, %, dS/m, pH |

### 🖼 Example Data (JSON)

```json
{
  "info": {"ResultOfGrowthLevel": "Low", "Place": "베란다(B)"},
  "plant": {"PlantName": "선인장", "PlantClass": "건생식물"},
  "sensor": {"AirTemperature": "31.2", "AirHumidity": "41.9", "Co2": "1500"},
  "watering": {"IrrigationState": "관수중", "AmtIrrigation": "65"}
}
```

### 🧠 Usage in This Project

* **텍스트 필드**: 식물명, 생육단계, 환경 → 임베딩
* **이미지**: Gemini API로 캡션 추출 후 텍스트화 → 텍스트 임베딩과 결합
* **센서**: `sensor` 키의 수치값들을 TabNet을 통해 latent vector로 변환
* **멀티벡터 구조**: 하나의 문서(`json`)에 대해 텍스트·이미지·센서 벡터를 동시에 저장

---

## 7. Pipeline
```
사용자 입력 (이미지 + 텍스트)
        │
        ▼
1️⃣ 이미지 캡션 생성 (Gemini API)
   └─ 이미지에서 식물의 상태를 자연어로 요약
      (예: "잎 끝이 갈색이고 잎이 처져 있음")
        │
        ▼
2️⃣ 쿼리 구성
   ├─ 사용자 텍스트 + 이미지 캡션 결합
   │   예: "보스턴고사리 잎이 말라가요. (이미지: 잎 끝이 갈색이고 처져 있음)"
   └─ HuggingFace(BGE-m3-ko) 임베딩 → text_vector(1024D)
        │
        ▼
3️⃣ Milvus 검색
   ├─ Step1: 같은 식물 중 “유사한 환경(문장 유사도)” 상위 K개  
   ├─ Step2: 같은 식물 중 “High 성장” 사례 K개  
   ├─ Step3: 같은 식물 중 “Low/DIE 성장” 사례 K개  
        │
        ▼
4️⃣ 환경 평균 계산
   - 각 그룹의 `sensor_vector` 평균 계산  
   - 출력: `current_avg`, `high_avg`, `low_avg`
        │
        ▼
5️⃣ Reasoning (LLM)
   LLM 입력 구성 요소:
   ① 사용자 입력 텍스트  
   ② 이미지 캡션(Gemini)  
   ③ 세 그룹의 센서 평균표 (`current_avg`, `high_avg`, `low_avg`)
        │
        ▼
6️⃣ LLM 출력
   🌿 “현재 상태 요약 + 더 잘 자란/못 자란 환경 비교 + 개선 조언” 생성
```
