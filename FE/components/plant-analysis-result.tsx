"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Textarea } from "@/components/ui/textarea"
import { Label } from "@/components/ui/label"
import { ArrowLeft, Droplets, Sun, Thermometer, AlertCircle, Leaf, Cloud, TrendingUp, Wind, Sprout, ThumbsUp, Minus, ThumbsDown} from "lucide-react"
import { PlantAnalysisResultData, MetricScore } from "@/types/plant"
import { toast } from "sonner"

interface PlantAnalysisResultProps {
  data: PlantAnalysisResultData
  onReset: () => void
}

// 센서 매핑 정보 (백엔드 키 -> 프론트엔드 아이콘/라벨)
const SENSOR_MAP: Record<string, { label: string; icon: any; color: string }> = {
  "AirTemperature": { label: "공기 온도", icon: Thermometer, color: "text-red-600" },
  "AirHumidity": { label: "공기 습도", icon: Droplets, color: "text-sky-600" },
  "Co2": { label: "CO2 농도", icon: Cloud, color: "text-gray-600" },
  "Quantum": { label: "광량 (PPFD)", icon: Sun, color: "text-yellow-600" },
  "HighSoilTemp": { label: "토양 온도 (상)", icon: Thermometer, color: "text-orange-600" },
  "HighSoilHumi": { label: "토양 습도 (상)", icon: Sprout, color: "text-green-600" },
  "LowSoilTemp": { label: "토양 온도 (하)", icon: Thermometer, color: "text-orange-700" },
  "LowSoilHumi": { label: "토양 습도 (하)", icon: Sprout, color: "text-green-700" },
}

export default function PlantAnalysisResult({ data, onReset }: PlantAnalysisResultProps) {
  const [feedbackType, setFeedbackType] = useState<string | null>(null)
  const [feedbackComment, setFeedbackComment] = useState("")
  const [isFeedbackSubmitting, setIsFeedbackSubmitting] = useState(false)
  const [isFeedbackSubmitted, setIsFeedbackSubmitted] = useState(false)

  const handleFeedbackSubmit = async () => {
      if (!feedbackType) {
          toast.error("결과(호전됨/유지/악화)를 선택해주세요.")
          return
      }

      setIsFeedbackSubmitting(true)
      try {
          // 백엔드 API 호출 (PATCH)
          const response = await fetch(`/api/proxy/v1/plants/history/${data.logId}/feedback`, {
              method: "PATCH",
              headers: {
                   "Content-Type": "application/json",
            },
            body: JSON.stringify({
              feedbackType: feedbackType, // "IMPROVED", "NO_CHANGE", "WORSENED"
              comment: feedbackComment
            }),
        })

        if (!response.ok) throw new Error("전송 실패")

        toast.success("피드백이 등록되었습니다! 감사합니다.")
        setIsFeedbackSubmitted(true)
    } catch (error) {
        console.error(error)
        toast.error("피드백 등록 중 오류가 발생했습니다.")
    } finally {
          setIsFeedbackSubmitting(false)
    }
  }

  return (
    <div className="container mx-auto max-w-5xl px-4 py-8">
      {/* Header */}
      <div className="mb-6">
        <Button variant="ghost" onClick={onReset} className="mb-4 gap-2 text-muted-foreground hover:text-foreground">
          <ArrowLeft className="h-4 w-4" />
          새로운 분석
        </Button>
        <div className="flex items-center gap-3">
          <div className="rounded-full bg-primary p-3">
            <Leaf className="h-6 w-6 text-primary-foreground" />
          </div>
          <div>
            <h1 className="text-3xl font-bold text-foreground">{data.plantName} 분석 결과</h1>
            <p className="text-muted-foreground">AI가 분석한 식물 상태입니다</p>
          </div>
        </div>
      </div>

      {/* 1. 전체 점수 카드 */}
      <Card className="mb-6 border-2 bg-gradient-to-br from-primary/5 to-accent/5 p-6">
        <div className="mb-6 flex items-center justify-between">
          <div>
            <h2 className="mb-1 text-2xl font-bold text-foreground">전체 건강도</h2>
            <p className="text-sm text-muted-foreground">종합 점수</p>
          </div>
          <div className="flex items-center gap-3">
            <div className="text-right">
              <div className="text-5xl font-bold text-primary">{data.overallScore}</div>
              <div className="text-sm text-muted-foreground">/ 100</div>
            </div>
            <div className="rounded-full bg-primary p-4">
              <TrendingUp className="h-8 w-8 text-primary-foreground" />
            </div>
          </div>
        </div>

        <div className="space-y-4 border-t pt-6">
          <h3 className="mb-4 text-base font-semibold text-foreground">상태 지표 (잘 자란 식물 대비)</h3>
          <div className="grid gap-4 sm:grid-cols-2">
            {data.metricScores.map((metric: MetricScore) => {
              const info = SENSOR_MAP[metric.sensorName] || { label: metric.sensorName, icon: AlertCircle, color: "text-gray-500" }
              const Icon = info.icon
              
              return (
                <div key={metric.sensorName} className="bg-white/50 p-3 rounded-lg border">
                  <div className="mb-2 flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <Icon className={`h-4 w-4 ${info.color}`} />
                      <span className="text-sm font-medium text-foreground">{info.label}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Badge variant={metric.status === "적정" ? "outline" : "destructive"} className="text-xs">
                        {metric.status}
                      </Badge>
                      <span className="text-sm font-semibold text-foreground">{metric.score}점</span>
                    </div>
                  </div>
                  <div className="h-2 overflow-hidden rounded-full bg-secondary">
                    <div
                      className={`h-full rounded-full transition-all ${metric.score > 80 ? 'bg-green-500' : metric.score > 50 ? 'bg-yellow-500' : 'bg-red-500'}`}
                      style={{ width: `${metric.score}%` }}
                    />
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      </Card>

      {/* 2. 종합 분석 (Analysis) */}
      <div className="mb-6">
        <h2 className="mb-4 text-2xl font-bold text-foreground">종합 분석</h2>
        <div className="space-y-4">
          {/* 키워드 배지 */}
          <div className="flex flex-wrap gap-2">
            {data.keywords.map((keyword, i) => (
              <Badge key={i} variant="secondary" className="px-3 py-1 text-sm">
                #{keyword}
              </Badge>
            ))}
          </div>

          <Card className="border-2 bg-gradient-to-br from-secondary/20 to-accent/10 p-6">
             <div className="mb-4 text-sm text-muted-foreground bg-white/60 p-3 rounded-md border">
                🔍 <strong>이미지 캡션:</strong> {data.caption}
             </div>
            <p className="text-pretty leading-relaxed text-foreground whitespace-pre-wrap">
              {data.analysis}
            </p>
          </Card>
        </div>
      </div>

      {/* 3. 세부 관리 가이드 (Top 3 Issues) */}
      <div className="mb-6">
        <h2 className="mb-4 text-2xl font-bold text-foreground">세부 관리 가이드 (Top 3 이슈)</h2>
        <div className="grid gap-4 md:grid-cols-3">
          {data.careGuide.map((guide, index) => (
            <Card key={index} className="border-2 p-5 transition-all hover:border-primary hover:shadow-md">
              <div className="mb-3 flex items-center justify-between">
                <div className="rounded-full bg-primary/10 p-2">
                  <AlertCircle className="h-5 w-5 text-primary" />
                </div>
                <Badge variant="default" className="text-xs">중요</Badge>
              </div>
              <h3 className="mb-2 font-semibold text-foreground">{guide.issue}</h3>
              <p className="text-sm leading-relaxed text-muted-foreground">{guide.content}</p>
            </Card>
          ))}
        </div>
      </div>

      {/* 4. 유사한 식물 사례 */}
      <div>
        <h2 className="mb-4 text-2xl font-bold text-foreground">유사한 식물 사례</h2>
        <Card className="border-2 p-6">
          <p className="mb-4 text-sm text-muted-foreground">현재 식물과 상태가 가장 유사한 데이터베이스 이미지입니다.</p>
          <div className="grid grid-cols-3 gap-4">
            {/* 👇 여기 변수명을 (image, index)로 하셨다면 */}
            {data.similarImages.map((image, index) => (
              <div
                key={index}
                className="overflow-hidden rounded-lg border-2 border-border transition-all hover:border-primary hover:shadow-md aspect-square relative bg-gray-100"
              >
                {/* 👇 내부에서도 똑같이 'image'를 써야 합니다. (imageName X) */}
                <img
                  src={`http://localhost:8080/images/${image}`}
                  alt={`Similar plant ${index + 1}`}
                  className="aspect-square w-full object-cover"
                />
              </div>
            ))}
          </div>
        </Card>
      </div>
      
    <div className="mb-8 mt-12">
                <h2 className="mb-4 text-2xl font-bold text-foreground">결과 피드백</h2>
                <Card className="border-2 p-6 bg-muted/20">
                    {!isFeedbackSubmitted ? (
                        <div className="space-y-4">
                            <p className="text-sm text-muted-foreground mb-4">
                                이 분석과 가이드를 따라한 후, 식물의 상태가 어떻게 변했나요? 
                                여러분의 데이터가 더 정확한 AI를 만듭니다.
                            </p>
                            
                            <div className="grid grid-cols-3 gap-3 mb-4">
                                <Button 
                                    variant={feedbackType === "IMPROVED" ? "default" : "outline"} 
                                    className="h-20 flex flex-col gap-2"
                                    onClick={() => setFeedbackType("IMPROVED")}
                                >
                                    <ThumbsUp className="h-6 w-6" />
                                    <span>좋아졌어요</span>
                                </Button>
                                <Button 
                                    variant={feedbackType === "NO_CHANGE" ? "default" : "outline"} 
                                    className="h-20 flex flex-col gap-2"
                                    onClick={() => setFeedbackType("NO_CHANGE")}
                                >
                                    <Minus className="h-6 w-6" />
                                    <span>변화 없음</span>
                                </Button>
                                <Button 
                                    variant={feedbackType === "WORSENED" ? "default" : "outline"} 
                                    className="h-20 flex flex-col gap-2"
                                    onClick={() => setFeedbackType("WORSENED")}
                                >
                                    <ThumbsDown className="h-6 w-6" />
                                    <span>나빠졌어요</span>
                                </Button>
                            </div>

                            <div className="space-y-2">
                                <Label htmlFor="feedback-comment">상세 후기 (선택)</Label>
                                <Textarea 
                                    id="feedback-comment" 
                                    placeholder="어떤 조언이 도움이 되었나요? 혹은 어떤 점이 아쉬웠나요?"
                                    value={feedbackComment}
                                    onChange={(e) => setFeedbackComment(e.target.value)}
                                    className="resize-none bg-white"
                                />
                            </div>

                            <div className="flex justify-end">
                                <Button 
                                    onClick={handleFeedbackSubmit} 
                                    disabled={isFeedbackSubmitting || !feedbackType}
                                >
                                    {isFeedbackSubmitting ? "등록 중..." : "피드백 등록하기"}
                                </Button>
                            </div>
                        </div>
                    ) : (
                        <div className="py-8 text-center text-muted-foreground">
                            <div className="flex justify-center mb-2">
                                <div className="rounded-full bg-green-100 p-3 text-green-600">
                                    <ThumbsUp className="h-6 w-6" />
                                </div>
                            </div>
                            <h3 className="text-lg font-semibold text-foreground">소중한 의견 감사합니다!</h3>
                            <p className="text-sm">보내주신 피드백은 서비스 개선에 활용됩니다.</p>
                        </div>
                    )}
                </Card>
      </div>

      {/* Action Button */}
      <div className="mt-8 text-center">
        <Button onClick={onReset} size="lg" className="gap-2">
          <ArrowLeft className="h-4 w-4" />
          다른 식물 분석하기
        </Button>
      </div>
    </div>
  )
}