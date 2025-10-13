# 5️⃣ 검색 테스트

# 1. 검색 쿼리 정의 (한국어로 질문)
#query = "이 식물은 선명하고 짙은 녹색을 띠며, 어떠한 황변이나 갈변 없이 깨끗한 상태를 유지합니다. 잎들은 처짐 없이 탄력 있게 솟아 있으며, 견고하게 형태를 유지하여 전반적으로 매우 풍성하고 활기찬 생명력이 넘치는 모습으로, 왕성한 성장을 시사합니다."
query = "베란다, 스파티필름, 이 식물은 전반적으로 활력이 낮은 상태를 보입니다. 잎들은 생기를 잃고 탁한 녹색을 띠고 있으며, 상당수의 잎에서 누렇게 변색되거나 갈색으로 말라가는 흔적이 뚜렷하게 관찰됩니다. 특히 많은 잎들이 탄력을 잃고 아래로 축 처져 있거나 가장자리가 안쪽으로 말리는 등 시든 모습을 보이며, 전체적으로 고사하는 부분이 많아 활력이 현저히 저하된 상태임을 알 수 있습니다."
print(f"🔍 검색 쿼리: {query}\n")

# 2. 유사도 검색 수행
try:
    retrieved_docs = vectorstore.similarity_search(query, k=3)

    # 3. 검색 결과 출력
    if retrieved_docs:
        print(f"✅ 검색 결과 (상위 {len(retrieved_docs)}개):")
        for i, doc in enumerate(retrieved_docs):
            print(f"\n--- 결과 {i+1} ---")
            print(f"➡️ 이미지 이름: {doc.metadata.get('image_name', 'N/A')}")
            print(f"➡️ 식물 이름: {doc.metadata.get('plant_name', 'N/A')}")
            print(f"➡️ 생장 수준: {doc.metadata.get('growth_level', 'N/A')}")
            print("➡️ 문서 내용 (일부):")
            # 전체 내용 대신 첫 200자만 출력
            print(doc.page_content[:200] + "...")
    else:
        print("⚠️ 검색 결과가 없습니다. 컬렉션이 비어 있거나 쿼리와 일치하는 문서가 없습니다.")

except Exception as e:
    print(f"❌ 검색 중 오류 발생: {repr(e)}")