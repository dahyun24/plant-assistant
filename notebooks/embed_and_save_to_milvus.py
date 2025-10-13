# 1️⃣ 필수 라이브러리 설치
!pip install langchain-huggingface langchain-milvus pymilvus tqdm

import json
from pathlib import Path
from tqdm import tqdm
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_milvus import Milvus
from langchain.docstore.document import Document

# 2️⃣ 임베딩 모델 및 Milvus 연결
embeddings = HuggingFaceEmbeddings(model_name="dragonkue/BGE-m3-ko")

# 0) 임베딩 & Milvus 연결
embeddings = HuggingFaceEmbeddings(model_name="dragonkue/BGE-m3-ko")

vectorstore = Milvus(
    embedding_function=embeddings,
    connection_args={"uri": "http://standalone:19530"},
    collection_name="plant_combined_vector",
    drop_old=False,  # 처음 완전 초기화가 필요하면 True로 한 번만 실행하고 다시 False로 변경
)

# 1) 경로
caption_paths = [
    Path("전체보스턴고사리_캡션.json"),
    Path("전체스파티필럼_캡션2.json"),
]

BATCH_SIZE = 100

def folder_for_caption(caption_path: Path) -> Path | None:
    name = caption_path.name
    if "보스턴고사리" in name: return Path("보스턴고사리")
    if "스파티필럼" in name:   return Path("스파티필럼")
    return None

total_added = 0

for caption_path in caption_paths:
    with open(caption_path, "r", encoding="utf-8") as f:
        caption_data = json.load(f)

    json_dir = folder_for_caption(caption_path)
    if json_dir is None:
        print(f"⚠️ 폴더 매핑 실패: {caption_path.name} (스킵)")
        continue

    batch_docs = []
    for idx, item in enumerate(tqdm(caption_data, desc=f"Processing {caption_path.name}"), start=1):
        image_name = item.get("image_name")
        caption_text = item.get("Gemini_Caption", "")

        if not image_name:
            continue

        json_file = json_dir / f"{Path(image_name).stem}.json"
        if not json_file.exists():
            # 필요한 경우 여기서 로그
            # print(f"⏭️ JSON 없음: {json_file}")
            continue$

        try:
            with open(json_file, "r", encoding="utf-8") as jf:
                data = json.load(jf)

            text = (
                f"{data['plant']['PlantName']}는 "
                f"{data['info']['Place']}에서 자라는 {data['plant']['PlantClass']}이며, "
                f"환경은 {data['plant']['Environment']} 상태이고 "
                f"생장 결과는 {data['info']['ResultOfGrowthLevel']}이다. "
                f"현재 {data['watering']['IrrigationState']} 상태이고, "
                f"관수량은 {data['watering']['AmtIrrigation']}ml이다."
            )

            combined_text = f"[식물 정보]\n{text}\n\n[이미지 설명]\n{caption_text}"

            doc = Document(
                page_content=combined_text,
                metadata={
                    "image_name": image_name,
                    "plant_name": data["plant"]["PlantName"],
                    "growth_level": data["info"]["ResultOfGrowthLevel"],
                    "place": data["info"]["Place"],
                },
            )
            batch_docs.append(doc)

        except KeyError as e:
            # 스키마 누락 방지용 로깅
            print(f"⏭️ KeyError {e} @ {json_file.name} (스킵)")
            continue

        # 배치 저장
        if len(batch_docs) >= BATCH_SIZE:
            try:
                vectorstore.add_documents(batch_docs)
                total_added += len(batch_docs)
                print(f"✅ 배치 저장: +{len(batch_docs)}개 (누적 {total_added})")
            except Exception as e:
                print(f"❌ 배치 저장 오류: {repr(e)}")
                # 필요 시 재시도 로직/백오프 추가 가능
            finally:
                batch_docs = []

    # 남은 문서 저장
    if batch_docs:
        try:
            vectorstore.add_documents(batch_docs)
            total_added += len(batch_docs)
            print(f"✅ 마지막 배치 저장: +{len(batch_docs)}개 (누적 {total_added})")
        except Exception as e:
            print(f"❌ 마지막 배치 저장 오류: {repr(e)}")

print("저장 루프 종료")
