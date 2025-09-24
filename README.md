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
- **텍스트 정보** → OpenAI `text-embedding-3-small`  
- **이미지 데이터** → BLIP/LLaVA로 캡션 생성 후 텍스트 임베딩
  - *직접 모델을 만들어서 식물 이미지에 맞는 텍스트를 추출할 수 있도록!  
- **센서 데이터** → TabNet (또는 문자열 변환 후 임베딩)  

### (2) 저장소 구조
- **Combined Vector Store**: 텍스트 + 이미지 요약을 같은 공간에 저장  
- **Multi-Vector Store**: 문서 단위로 Combined Store 임베딩과 센서 벡터를 연결  

### (3) Retrieval 전략
- 기본적으로 **Multi-Vector Retrieval**을 채택  
- 질의 유형에 따라 modality별 가중치 조정  
  - 예: 환경 관련 → 센서 벡터 강화  
  - 예: 증상 관련 → 이미지 요약 강화  
- 추후 Cross-Encoder reranker 확장 가능  

### (4) Fusion
- 초기: 단순 **score aggregation (가중치 합산)**  
- 확장: **Late Fusion** 또는 **Cross-Encoder reranking**  

---

## 4. System Architecture
```text
사용자 질의 (텍스트/이미지/센서)
   ↓
[Embedding 단계]
   - 텍스트: text-embedding-3-small
   - 이미지: 캡션 생성 → 텍스트 임베딩
   - 센서: TabNet 또는 텍스트화
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
