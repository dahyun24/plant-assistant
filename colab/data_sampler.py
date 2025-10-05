# data_sampler.py

import os
import json
import pandas as pd
import random

JSON_DIR = "02.라벨링데이터/VL_A.화초_3.습생식물_08.보스턴고사리"

INTERMEDIATE_FILE = "bostongosari.jsonl"

TARGET_SAMPLE_COUNTS = {
    "High": 500,
    "DIE:": 140,
    "Low": 500
}
TARGET_CATEGORIES = list(TARGET_SAMPLE_COUNTS.keys())

def load_and_sample_data():
    """
    JSON 파일에서 성장 레벨& 이미지 이름을 추출, TARGET_SAMPLE_COUNTS에 따라 샘플링
    결과는 INTERMEDIATE_FILE에 JSONL 형식으로 저장
    """
    data_list = []
    
    # 1. JSON 파일 목록 읽기
    json_files = [f for f in os.listdir(JSON_DIR) if f.endswith('.json')]
    if not json_files:
        print(f"❌ 오류: 경로 ({JSON_DIR})에서 JSON 파일을 찾을 수 없습니다. 경로를 확인하세요.")
        return None

    print(f"총 {len(json_files)}개의 JSON 파일을 발견했습니다. 데이터 추출을 시작합니다...")
    
    # 2. 모든 JSON 파일에서 필요한 정보 추출
    for filename in json_files:
        filepath = os.path.join(JSON_DIR, filename)
        try:
            with open(filepath, 'r', encoding='utf-8') as f:
                data = json.load(f)
                
                level = data.get('info', {}).get('ResultOfGrowthLevel')
                image_name = data.get('picInfo', {}).get('ImageName')
                
                if level in TARGET_CATEGORIES and image_name:
                    data_list.append({
                        'image_name': image_name,
                        'growth_level': level
                    })
        except Exception as e:
            print(f"  ⚠️ JSON 파일 로드/파싱 오류 ({filename}): {e}. 이 파일은 건너뜜.")

    df = pd.DataFrame(data_list)
    if df.empty:
        print("❌ 오류: 유효한 성장 레벨 데이터가 JSON 파일에서 발견되지 않았습니다.")
        return None

    # 3. 카테고리별로 목표 개수만큼 샘플링
    sampled_list = []
    random.seed(42) # 일관된 샘플링을 위해 시드 고정
    
    for level in TARGET_CATEGORIES:
        desired_count = TARGET_SAMPLE_COUNTS.get(level, 0)
        
        category_df = df[df['growth_level'] == level]
        sample_n = min(desired_count, len(category_df))
        
        if sample_n > 0:
            # random.sample()을 사용하여 DataFrame 인덱스에서 샘플링 (안정성)
            sampled_indices = random.sample(list(category_df.index), sample_n)
            sampled_data = category_df.loc[sampled_indices]
            sampled_list.append(sampled_data)
            print(f"'{level}' 카테고리에서 목표 {desired_count}개 중 {sample_n}개의 이미지를 선별했습니다.")
        else:
            print(f"'{level}' 카테고리에 데이터가 부족하거나 목표 개수가 0입니다. (0개 선별)")

    final_df = pd.concat(sampled_list).reset_index(drop=True)
    print(f"\n 총 {len(final_df)}개의 이미지가 최종 캡션 생성 대상으로 선별되었습니다.")

    # 4. 선별된 목록을 중간 파일로 저장 (JSON Lines 형식)
    final_df[['image_name', 'growth_level']].to_json(
        INTERMEDIATE_FILE, 
        orient='records', 
        lines=True, 
        force_ascii=False
    )
    print(f"\n✅ 1단계 완료: 선별된 이미지 목록이 '{INTERMEDIATE_FILE}'로 저장되었습니다.")
    
    return final_df


if __name__ == "__main__":
    load_and_sample_data()
