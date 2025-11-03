import numpy as np
from pymilvus import connections, Collection
from langchain_huggingface import HuggingFaceEmbeddings

# ======================================
# 1️⃣ Milvus 연결
# ======================================
connections.connect(alias="default", uri="http://localhost:19530")
collection_name = "plant_multi_vector"
collection = Collection(collection_name)
collection.load()
print(f"✅ Collection loaded: {collection_name}")

# ======================================
# 2️⃣ 임베딩 모델 로드
# ======================================
embeddings = HuggingFaceEmbeddings(model_name="dragonkue/BGE-m3-ko", show_progress=False)

# ======================================
# 3️⃣ 성장 단계 정의
# ======================================
GROWTH_RANK = {"DIE": 0, "Low": 1, "Medium": 2, "High": 3}

# ======================================
# 4️⃣ 헬퍼 함수
# ======================================
def growth_rank(level: str) -> int:
    return GROWTH_RANK.get(level.strip().capitalize(), -1)

def vector_mean(vectors):
    """Milvus sensor_vector 평균 계산"""
    if not vectors:
        return None
    arr = np.array(vectors)
    return np.mean(arr, axis=0)

def explain_sensor_diff(curr_avg, pos_avg, field_names):
    """센서 평균 차이를 해석하고 조언 생성"""
    advice = []
    for i, key in enumerate(field_names):
        c, p = curr_avg[i], pos_avg[i]
        diff = round(c - p, 2)
        if abs(diff) < 0.1:
            continue
        if "Temp" in key and c > p:
            advice.append(f"{key}: 온도가 높아요(+{diff}). 약간 낮춰주세요.")
        elif "Temp" in key and c < p:
            advice.append(f"{key}: 온도가 낮아요({diff}). 온도를 높이세요.")
        elif "Humidity" in key and c < p:
            advice.append(f"{key}: 습도가 낮아요({abs(diff)}). 가습이나 관수를 늘려주세요.")
        elif "Humidity" in key and c > p:
            advice.append(f"{key}: 습도가 높아요(+{diff}). 환기를 늘리세요.")
        elif "PH" in key and abs(diff) > 0.3:
            advice.append(f"{key}: pH 편차({diff})가 커요. 적정 범위로 조정하세요.")
        elif "EC" in key and c > p + 0.5:
            advice.append(f"{key}: 비료 농도(EC)가 높아요. 희석해서 공급하세요.")
    return advice


# ======================================
# 5️⃣ STEP 1 — 문장 기반 유사한 식물 검색
# ======================================
def search_similar_symptoms(query_text, plant_name, top_k=10):
    """같은 식물 중에서 문장 임베딩 유사도 기반 검색"""
    query_emb = np.array(embeddings.embed_query(query_text)).tolist()
    expr = f"plant_name == '{plant_name}'"
    params = {"metric_type": "COSINE", "params": {"nprobe": 32}}

    results = collection.search(
        data=[query_emb],
        anns_field="text_vector",
        param=params,
        limit=top_k,
        expr=expr,
        output_fields=["plant_name", "growth_level", "sensor_vector", "image_name"]
    )

    hits = results[0]
    if not hits:
        print("⚠️ 유사한 데이터 없음")
        return []

    print(f"\n🔍 '{plant_name}' 유사한 증상 Top-{top_k}")
    samples = []
    for i, hit in enumerate(hits, 1):
        sim = 1 - hit.distance
        g = hit.entity.get("growth_level")
        img = hit.entity.get("image_name")
        print(f"{i:>2}. [{g}] sim={sim:.3f} | {img}")
        samples.append({
            "growth_level": g,
            "sensor_vector": hit.entity.get("sensor_vector")
        })
    return samples


# ======================================
# 6️⃣ STEP 2 — 성장 단계별 그룹 검색
# ======================================
def search_growth_groups(plant_name, user_growth_level, top_k=10):
    """현재 성장단계보다 높고 낮은 그룹 각각 검색"""
    user_rank = growth_rank(user_growth_level)
    higher_levels = [lvl for lvl, r in GROWTH_RANK.items() if r > user_rank]
    lower_levels = [lvl for lvl, r in GROWTH_RANK.items() if r < user_rank]
    results = {"positive": [], "negative": []}

    if higher_levels:
        pos_expr = f"plant_name == '{plant_name}' and (" + " or ".join([f"growth_level == '{lvl}'" for lvl in higher_levels]) + ")"
        pos = collection.query(expr=pos_expr, output_fields=["growth_level", "sensor_vector"], limit=top_k)
        results["positive"] = pos

    if lower_levels:
        neg_expr = f"plant_name == '{plant_name}' and (" + " or ".join([f"growth_level == '{lvl}'" for lvl in lower_levels]) + ")"
        neg = collection.query(expr=neg_expr, output_fields=["growth_level", "sensor_vector"], limit=top_k)
        results["negative"] = neg

    print(f"\n✅ '{plant_name}' 성장단계별 그룹 — 현재: {user_growth_level}")
    print(f"📈 더 잘 자란: {len(results['positive'])}개 / 📉 더 못 자란: {len(results['negative'])}개")
    return results


# ======================================
# 7️⃣ STEP 3 — 센서 비교 및 조언 생성
# ======================================
def compare_environment(similar_samples, groups):
    """유사한 식물 평균 vs 잘 자란 그룹 평균 비교 + 평균값 출력"""
    curr_avg = vector_mean([s["sensor_vector"] for s in similar_samples])
    pos_avg = vector_mean([p["sensor_vector"] for p in groups["positive"]])
    neg_avg = vector_mean([n["sensor_vector"] for n in groups["negative"]])

    if curr_avg is None or pos_avg is None:
        print("⚠️ 평균 계산 불가 (데이터 부족)")
        return []

    sensor_keys = ["AirTemperature","AirHumidity","Co2","Quantum",
                   "HighSoilTemp","HighSoilHumi","LowSoilTemp","LowSoilHumi"]

    # --- 📊 평균값 비교 테이블 출력 ---
    print("\n📊 [센서 평균값 비교]")
    print(f"{'센서항목':<14} {'유사상태':>10} {'잘 자란':>10} {'못 자란':>10}")
    print("-" * 60)
    for i, key in enumerate(sensor_keys):
        c = round(curr_avg[i], 2)
        p = round(pos_avg[i], 2) if pos_avg is not None else "-"
        n = round(neg_avg[i], 2) if neg_avg is not None else "-"
        print(f"{key:<20} {c:>12} {p:>12} {n:>12}")
    print("-" * 60)

    # --- 🌿 조언 생성 ---
    advice = explain_sensor_diff(curr_avg, pos_avg, sensor_keys)

    print("\n🌡 [환경 비교 결과]")
    for i, msg in enumerate(advice, 1):
        print(f"{i}. {msg}")

    return advice



# ======================================
# 8️⃣ 통합 실행
# ======================================
def analyze_plant_condition(query_text, plant_name, user_growth_level, top_k=10):
    print("\n🚀 STEP 1: 유사 증상 검색")
    similar_samples = search_similar_symptoms(query_text, plant_name, top_k=top_k)

    print("\n🚀 STEP 2: 성장단계별 그룹 검색")
    groups = search_growth_groups(plant_name, user_growth_level, top_k=top_k)

    print("\n🚀 STEP 3: 환경 비교 및 조언 생성")
    advice = compare_environment(similar_samples, groups)

    print("\n✅ 최종 개선 조언 요약:")
    for a in advice:
        print("-", a)
    return advice


# ======================================
# 9️⃣ 테스트 실행
# ======================================
if __name__ == "__main__":
    query_text = (
        "잎이 전체적으로 갈색으로 말라가고 있고 활력이 떨어집니다. "
        "식물의 생장이 둔화된 것 같습니다."
    )
    plant_name = "보스턴고사리"
    user_growth_level = "Low"

    analyze_plant_condition(query_text, plant_name, user_growth_level, top_k=10)
