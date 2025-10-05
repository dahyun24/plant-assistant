# caption_generator.py

import os
import json
import time
import pandas as pd
from google import genai
from PIL import Image

API_KEY = os.environ.get("GEMINI_API_KEY")

# 이미지 파일(*.jpg, *.png)들이 저장된 폴더 경로
IMAGE_DIR = "/content/drive/MyDrive/plant_images/스파티필럼/"

# 1단계에서 저장한 선별 목록 파일 경로 (spatipilam.jsonl)
INTERMEDIATE_FILE = "/content/drive/MyDrive/plant_images/spatipilam.jsonl"

# 캡션 결과를 저장할 최종 학습 데이터셋 JSON 파일 경로
OUTPUT_FILE = "/content/drive/MyDrive/plant_images/스파티필럼_캡션.json"

# API 요청 한도를 위한 설정
DELAY_PER_REQUEST = 4
MODEL_NAME = "gemini-2.5-flash"

# 사용자 정의 캡션 생성 프롬프트 템플릿
CUSTOM_PROMPT_TEMPLATE = (
    "이 식물의 **실제 성장 레벨은 '{growth_level}'**입니다. 이 정보를 바탕으로, 이미지 속 식물의 건강 상태만을 설명하는 문장을 하나 생성하세요. "
    "배경 정보는 모두 제외하고, 잎의 색상 변화, 처짐 여부, 전반적인 활력에 대해서만 상세히 서술하세요."
)

def generate_caption(client, image_path, prompt):
    """ 단일 이미지에 대한 캡션을 생성합니다. """
    try:
        image = Image.open(image_path)
        response = client.models.generate_content(
            model=MODEL_NAME,
            contents=[prompt, image]
        )
        return response.text.strip()

    except Exception as e:
        # ResourceExhaustedError를 포함한 모든 오류 처리
        if "ResourceExhausted" in str(e):
             return "ERROR_QUOTA_EXCEEDED"
        else:
             return f"ERROR: {str(e)}"

def save_checkpoint(data_list, output_file):
    """ 현재까지 처리된 데이터를 JSON 파일로 저장합니다 (체크포인트). """
    try:
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(data_list, f, ensure_ascii=False, indent=4)
    except Exception as e:
        print(f"\n체크포인트 저장 중 오류 발생: {e}")


def process_and_save():
    """ 선별된 목록을 불러와 캡션을 생성하고 중단 지점부터 이어서 처리합니다. """
    if not os.path.exists(INTERMEDIATE_FILE):
        print(f"오류: 1단계 파일 '{INTERMEDIATE_FILE}'이 존재하지 않습니다. Drive 경로와 파일명을 확인하세요.")
        return

    client = genai.Client()

    # 1. 선별 목록 불러오기 (JSON Lines 형식)
    try:
        df = pd.read_json(INTERMEDIATE_FILE, lines=True)
    except Exception as e:
        print(f"오류: 중간 파일 로드 중 오류 발생. 파일 형식(.jsonl)을 확인하세요. {e}")
        return

    final_dataset = []
    processed_image_names = set()

    # 2. 중단 지점 확인 및 기존 데이터 로드 (이어하기 기능)
    if os.path.exists(OUTPUT_FILE):
        try:
            with open(OUTPUT_FILE, 'r', encoding='utf-8') as f:
                final_dataset = json.load(f)
                processed_image_names = {item['image_name'] for item in final_dataset}
                print(f"\n이어하기 감지: 기존에 {len(final_dataset)}개의 캡션이 처리되었습니다.")
        except json.JSONDecodeError:
            print("\n기존 OUTPUT_FILE의 JSON 형식이 올바르지 않아 무시하고 새로 시작합니다.")
            final_dataset = []

    total_to_process = len(df)

    # 3. 캡션 생성 및 속도 조절 적용
    for index, row in df.iterrows():
        image_name = row['image_name']
        growth_level = row['growth_level']

        # 이미 처리된 이미지면 건너뛰기
        if image_name in processed_image_names:
            # print(f"--- 처리 완료됨: {image_name} ---")
            continue

        image_path = os.path.join(IMAGE_DIR, image_name)

        # 현재 진행 상황 표시 (건너뛴 항목 포함)
        current_count = len(final_dataset) + 1
        print(f"--- 처리 중 ({current_count}/{total_to_process}): {image_name} [Level: {growth_level}] ---")

        if not os.path.exists(image_path):
            print(f"  이미지 파일이 존재하지 않아 건너뜁니다: {image_path}")
            continue

        # ⭐️ 동적 프롬프트 생성
        final_prompt = CUSTOM_PROMPT_TEMPLATE.format(growth_level=growth_level)

        caption = generate_caption(client, image_path, final_prompt)

        if caption.startswith("ERROR_QUOTA_EXCEEDED"):
            print("\nAPI 요청 한도 초과! 작업을 중단합니다. 체크포인트를 저장합니다.")
            break # 루프 중단

        elif caption.startswith("ERROR"):
            print(f"  캡션 생성 오류 발생: {caption}")
            continue

        # 결과 저장
        final_dataset.append({
            "image_name": image_name,
            "original_growth_level": growth_level,
            "Gemini_Caption": caption
        })
        print(f"  캡션 생성 완료: {caption[:70]}...")

        # 속도 조절
        print(f"  잠시 대기 ({DELAY_PER_REQUEST}초) - RPM 제한 관리 중...")
        time.sleep(DELAY_PER_REQUEST)

        # 매 5개 처리마다 중간 저장
        if len(final_dataset) % 5 == 0:
             save_checkpoint(final_dataset, OUTPUT_FILE)
             print(f"  ⭐ 중간 체크포인트 저장 완료. (총 {len(final_dataset)}개)")

    # 작업 완료 또는 중단 시 최종 저장
    save_checkpoint(final_dataset, OUTPUT_FILE)

    print(f"\n\n========================================================")
    print(f"작업 완료/중단. 최종 데이터셋이 저장되었습니다.")
    print(f"저장 경로: {os.path.abspath(OUTPUT_FILE)}")
    print(f"최종 처리된 이미지 수: {len(final_dataset)}개")
    print(f"========================================================")


if __name__ == "__main__":
    process_and_save()
