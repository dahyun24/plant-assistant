export interface MetricScore {
  sensorName: string;
  score: number;
  status: string;
}

export interface CareGuide {
  issue: string;
  content: string;
}

export interface PlantAnalysisResultData {
  plantName: string;
  overallScore: number;
  metricScores: MetricScore[];
  caption: string;
  analysis: string;
  keywords: string[];
  careGuide: CareGuide[];
  similarImages: string[];
}

export interface ApiResponse {
  isSuccess: boolean;
  code: number;
  message: string;
  result: PlantAnalysisResultData;
}

export interface HistoryItem {
  id: number;
  plantName: string;
  growthLevel: string;
  userDescription: string;
  createdAt: string;
}

export interface HistoryListResponse {
  isSuccess: boolean;
  code: number;
  message: string;
  result: HistoryItem[];
}