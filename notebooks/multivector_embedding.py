import os
import json
import numpy as np
from pathlib import Path
from tqdm import tqdm
from typing import Optional
from pymilvus import connections, FieldSchema, CollectionSchema, DataType, Collection, utility
from langchain_huggingface import HuggingFaceEmbeddings

# Milvus 연결
connections.connect(alias="default", uri="http://localhost:19530")

sensor_fields = [
    "AirTemperature", "AirHumidity", "Co2", "Quantum",
    "HighSoilTemp", "HighSoilHumi", "LowSoilTemp", "LowSoilHumi"
]

embeddings = HuggingFaceEmbeddings(model_name="dragonkue/BGE-m3-ko", show_progress=False)

# Multi-Vector 컬렉션 생성
collection_name = "plant_multi_vector"

fields = [
    FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=True),
    FieldSchema(name="text_vector", dtype=DataType.FLOAT_VECTOR, dim=1024),
    FieldSchema(name="sensor_vector", dtype=DataType.FLOAT_VECTOR, dim=8),
    FieldSchema(name="plant_name", dtype=DataType.VARCHAR, max_length=50),
    FieldSchema(name="growth_level", dtype=DataType.VARCHAR, max_length=10),
    FieldSchema(name="place", dtype=DataType.VARCHAR, max_length=20),
    FieldSchema(name="image_name", dtype=DataType.VARCHAR, max_length=100),
]

schema = CollectionSchema(fields, description="Plant multi-vector collection (text + sensor)")

if collection_name not in utility.list_collections():
    collection = Collection(name=collection_name, schema=schema)
else:
    collection = Collection(name=collection_name)

print(f"✅ Collection ready: {collection_name}")


def folder_for_caption(caption_path: Path) -> Optional[Path]:
    """캡션 파일명 기준으로 JSON 폴더 경로 매핑"""
    name = caption_path.name
    if "보스턴고사리" in name:
        return Path("VL_A.화초_3.습생식물_08.보스턴고사리")
    if "스파티필럼" in name:
        return Path("VL_A.화초_2.중생식물_07.스파티필럼")
    return None


def extract_sensor_vector(json_path: Path) -> list[float]:
    """센서값 8개 추출"""
    with open(json_path, "r", encoding="utf-8") as f:
        data = json.load(f)
        sensor = data.get("sensor", {})
        vec = []
        for key in sensor_fields:
            try:
                vec.append(float(sensor.get(key, 0.0)))
            except ValueError:
                vec.append(0.0)
        return vec


def build_combined_text(data: dict, caption_text: str) -> str:
    """텍스트 임베딩용 문장 생성"""
    t = (
        f"{data['plant']['PlantName']}는 "
        f"{data['info']['Place']}에서 자라는 {data['plant']['PlantClass']}이며, "
        f"환경은 {data['plant']['Environment']} 상태이고 "
        f"생장 결과는 {data['info']['ResultOfGrowthLevel']}이다. "
        f"현재 {data['watering']['IrrigationState']} 상태이고, "
        f"관수량은 {data['watering']['AmtIrrigation']}ml이다."
    )
    return f"[식물 정보]\n{t}\n\n[이미지 설명]\n{caption_text}"

# 데이터 삽입 루프
caption_paths = [
    Path("전체보스턴고사리_캡션.json"),
    Path("전체스파티필럼_캡션2.json"),
]

BATCH_SIZE = 50
total_added = 0

for caption_path in caption_paths:
    json_dir = folder_for_caption(caption_path)
    if not json_dir:
        print(f"⚠️ 폴더 매핑 실패: {caption_path.name}")
        continue

    with open(caption_path, "r", encoding="utf-8") as f:
        caption_data = json.load(f)

    text_vecs, sensor_vecs, plant_names, growth_levels, places, image_names = [], [], [], [], [], []

    for idx, item in enumerate(tqdm(caption_data, desc=f"Processing {caption_path.name}"), start=1):
        image_name = item.get("image_name")
        caption_text = item.get("Gemini_Caption", "")
        if not image_name:
            continue

        json_file = json_dir / f"{Path(image_name).stem}.json"
        if not json_file.exists():
            continue

        with open(json_file, "r", encoding="utf-8") as jf:
            data = json.load(jf)

        combined_text = build_combined_text(data, caption_text)
        text_vector = np.array(embeddings.embed_query(combined_text))
        sensor_vector = np.array(extract_sensor_vector(json_file))

        text_vecs.append(text_vector.tolist())
        sensor_vecs.append(sensor_vector.tolist())
        plant_names.append(data["plant"]["PlantName"])
        growth_levels.append(data["info"]["ResultOfGrowthLevel"])
        places.append(data["info"]["Place"])
        image_names.append(image_name)

        if len(text_vecs) >= BATCH_SIZE:
            collection.insert([
                text_vecs, sensor_vecs, plant_names,
                growth_levels, places, image_names
            ])
            total_added += len(text_vecs)
            print(f"✅ 배치 저장 완료 (+{len(text_vecs)}개, 누적 {total_added})")
            text_vecs, sensor_vecs, plant_names, growth_levels, places, image_names = [], [], [], [], [], []

    # 남은 데이터 처리
    if text_vecs:
        collection.insert([
            text_vecs, sensor_vecs, plant_names,
            growth_levels, places, image_names
        ])
        total_added += len(text_vecs)
        print(f"✅ 마지막 배치 저장 (+{len(text_vecs)}개, 누적 {total_added})")

print(f"🎉 전체 완료! 총 {total_added}개 문서가 저장되었습니다.")

# 8️⃣ 인덱스 생성 및 컬렉션 로드
print("\n🔍 Milvus 인덱스 생성 및 컬렉션 로드 시작...")

# 1. text_vector (1024 dim) 인덱스 설정: IVF_FLAT (ANN 검색)
index_params_text = {
    "index_type": "IVF_FLAT", 
    "metric_type": "COSINE", 
    "params": {"nlist": 128}  # nlist는 데이터 크기에 따라 조정 가능
}

# 2. sensor_vector (8 dim) 인덱스 설정: FLAT (100% 정확도)
index_params_sensor = {
    "index_type": "FLAT", 
    "metric_type": "L2", 
    "params": {} 
}

# 인덱스가 없는 경우에만 생성
if not collection.has_index():
    print("⏳ 인덱스 생성 중... (데이터 양에 따라 시간이 걸릴 수 있습니다)")
    try:
        # text_vector에 인덱스 생성
        collection.create_index(field_name="text_vector", index_params=index_params_text)
        # sensor_vector에 인덱스 생성
        collection.create_index(field_name="sensor_vector", index_params=index_params_sensor)
        print("✅ 인덱스 생성 완료")
    except Exception as e:
        print(f"❌ 인덱스 생성 중 오류 발생: {e}")
else:
    print("✅ 인덱스가 이미 존재합니다. 재사용합니다.")
    
# 검색을 위해 컬렉션을 메모리에 로드 (필수)
try:
    collection.load()
    print("✅ 컬렉션 메모리 로드 완료: 이제 검색을 수행할 수 있습니다.")
except Exception as e:
    print(f"❌ 컬렉션 로드 중 오류 발생: {e}")