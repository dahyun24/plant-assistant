@ -1,79 +0,0 @@
# 1️⃣ 필수 라이브러리 설치
# !pip install langchain-huggingface langchain-milvus pymilvus tqdm

import json
from pathlib import Path
from tqdm import tqdm
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_milvus import Milvus
from langchain.docstore.document import Document

# 2️⃣ 임베딩 모델 및 Milvus 연결
embeddings = HuggingFaceEmbeddings(model_name="dragonkue/BGE-m3-ko")

vectorstore = Milvus(
    embedding_function=embeddings,
    connection_args={"uri": "http://localhost:19530"},
    collection_name="plant_combined_vector",
    drop_old=True
)

# 3️⃣ 파일 경로 설정
caption_paths = [
    Path("전체보스턴고사리_캡션.json"),
    Path("전체스파티필럼_캡션.json")
]

json_dir = Path("./")
# 4️⃣ 텍스트+캡션 결합 후 임베딩 및 Milvus 저장
documents = []

for caption_path in caption_paths:
    with open(caption_path, "r", encoding="utf-8") as f:
        caption_data = json.load(f)

    for item in tqdm(caption_data, desc=f"Processing {caption_path.name}"):
        image_name = item["image_name"]
        caption_text = item["Gemini_Caption"]

        json_file = json_dir / f"{Path(image_name).stem}.json"
        if not json_file.exists():
            continue

        with open(json_file, "r", encoding="utf-8") as jf:
            data = json.load(jf)

        try:
            text = (
                f"{data['plant']['PlantName']}는 "
                f"{data['info']['Place']}에서 자라는 {data['plant']['PlantClass']}이며, "
                f"환경은 {data['plant']['Environment']} 상태이고 "
                f"생장 결과는 {data['info']['ResultOfGrowthLevel']}이다. "
                f"현재 {data['watering']['IrrigationState']} 상태이고, "
                f"관수량은 {data['watering']['AmtIrrigation']}ml이다."
            )
        except KeyError:
            continue

        # 텍스트와 캡션 결합
        combined_text = (
            f"[식물 정보]\n{text}\n\n"
            f"[이미지 설명]\n{caption_text}"
        )

        # Document 객체 생성
        doc = Document(
            page_content=combined_text,
            metadata={
                "image_name": image_name,
                "plant_name": data["plant"]["PlantName"],
                "growth_level": data["info"]["ResultOfGrowthLevel"],
                "place": data["info"]["Place"],
            },
        )
        documents.append(doc)

# 5️⃣ Milvus에 임베딩 저장
vectorstore.add_documents(documents)

print(f"✅ 저장 완료: {len(documents)}개의 combined 벡터가 Milvus에 저장되었습니다.")