"use client"

import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { ArrowLeft, Droplets, Sun, Thermometer, AlertCircle, Leaf, Cloud, TrendingUp } from "lucide-react"

interface PlantAnalysisResultProps {
  plantType: string
  onReset: () => void
}

export default function PlantAnalysisResult({ plantType, onReset }: PlantAnalysisResultProps) {
  // Mock data - 실제로는 API에서 받아올 데이터
  const analysis = {
    status: "good",
    healthScore: 85,
    environment: {
      season: "가을",
      indoorTemp: 22,
      humidity: 65,
      lightCondition: "밝은 간접광",
    },
    summary:
      "전반적으로 건강한 상태입니다. 다만 잎 끝이 갈변되어 있어 수분 관리에 주의가 필요합니다. 현재 습도와 광량은 적절하며, 규칙적인 물주기로 더욱 건강하게 키울 수 있습니다.",
    contextualAdvice:
      "현재 가을철이고 실내 온도 22°C, 습도 65%는 보스턴고사리에게 이상적인 환경입니다. 비슷한 조건에서 키우는 다른 사용자들은 주 2-3회 물주기로 좋은 결과를 얻고 있습니다. 난방이 시작되면 가습기 사용을 권장합니다.",
    issues: [{ type: "warning", message: "이 식물은 전반적으로 활력이 있으며 대부분의 잎은 싱싱한 녹색을 띠고 있으나, 일부 잎 끝 부분에 노랗게 변색되거나 갈색으로 마른 흔적이 보입니다. 잎들이 심하게 처지지는 않았지만, 완벽한 활력을 보여주기에는 약간의 개선이 필요해 보입니다." }],
    statusMetrics: {
      CO2: 75,
      light: 85,
      temperature: 90,
      humidity: 80,
      soilTemp: 90,
      soilHumi: 87,
    },
    recommendations: [
      {
        icon: Droplets,
        title: "물주기",
        description: "토양이 빠르게 건조되는 편입니다. 흙 표면이 마르면 바로 충분히 물을 주세요.",
        priority: "high",
      },
      {
        icon: Sun,
        title: "햇빛",
        description: "현재 광량은 적정하나, 잎이 탈색된다면 커튼을 통한 간접광으로 조정하세요.",
        priority: "medium",
      },
      {
        icon: Thermometer,
        title: "온도",
        description: "18-24°C를 유지하는 것이 좋습니다",
        priority: "low",
      },
    ],
    similarImages: ["/healthy-boston-fern.jpg", "/boston-fern-care.jpg", "/indoor-fern-plant.jpg"],
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
            <h1 className="text-3xl font-bold text-foreground">{plantType} 분석 결과</h1>
            <p className="text-muted-foreground">AI가 분석한 식물 상태입니다</p>
          </div>
        </div>
      </div>

      <Card className="mb-6 border-2 bg-gradient-to-br from-primary/5 to-accent/5 p-6">
        <div className="mb-6 flex items-center justify-between">
          <div>
            <h2 className="mb-1 text-2xl font-bold text-foreground">전체 건강도</h2>
            <p className="text-sm text-muted-foreground">식물의 현재 상태 점수</p>
          </div>
          <div className="flex items-center gap-3">
            <div className="text-right">
              <div className="text-5xl font-bold text-primary">{analysis.healthScore}</div>
              <div className="text-sm text-muted-foreground">/ 100</div>
            </div>
            <div className="rounded-full bg-primary p-4">
              <TrendingUp className="h-8 w-8 text-primary-foreground" />
            </div>
          </div>
        </div>

        <div className="space-y-4 border-t pt-6">
          <h3 className="mb-4 text-base font-semibold text-foreground">상태 지표</h3>

          <div className="space-y-3">
            <div>
              <div className="mb-2 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Thermometer className="h-4 w-4 text-red-600" />
                  <span className="text-sm font-medium text-foreground">온도</span>
                </div>
                <span className="text-sm font-semibold text-foreground">{analysis.statusMetrics.temperature}%</span>
              </div>
              <div className="h-2 overflow-hidden rounded-full bg-secondary">
                <div
                  className="h-full rounded-full bg-gradient-to-r from-red-500 to-red-600 transition-all"
                  style={{ width: `${analysis.statusMetrics.temperature}%` }}
                />
              </div>
            </div>

            <div>
              <div className="mb-2 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Droplets className="h-4 w-4 text-sky-600" />
                  <span className="text-sm font-medium text-foreground">습도</span>
                </div>
                <span className="text-sm font-semibold text-foreground">{analysis.statusMetrics.humidity}%</span>
              </div>
              <div className="h-2 overflow-hidden rounded-full bg-secondary">
                <div
                  className="h-full rounded-full bg-gradient-to-r from-sky-500 to-sky-600 transition-all"
                  style={{ width: `${analysis.statusMetrics.humidity}%` }}
                />
              </div>
            </div>

            <div>
              <div className="mb-2 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Cloud className="h-4 w-4 text-blue-600" />
                  <span className="text-sm font-medium text-foreground">CO2</span>
                </div>
                <span className="text-sm font-semibold text-foreground">{analysis.statusMetrics.CO2}%</span>
              </div>
              <div className="h-2 overflow-hidden rounded-full bg-secondary">
                <div
                  className="h-full rounded-full bg-gradient-to-r from-blue-500 to-blue-600 transition-all"
                  style={{ width: `${analysis.statusMetrics.CO2}%` }}
                />
              </div>
            </div>

            <div>
              <div className="mb-2 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Sun className="h-4 w-4 text-yellow-600" />
                  <span className="text-sm font-medium text-foreground">광량</span>
                </div>
                <span className="text-sm font-semibold text-foreground">{analysis.statusMetrics.light}%</span>
              </div>
              <div className="h-2 overflow-hidden rounded-full bg-secondary">
                <div
                  className="h-full rounded-full bg-gradient-to-r from-yellow-500 to-yellow-600 transition-all"
                  style={{ width: `${analysis.statusMetrics.light}%` }}
                />
              </div>
            </div>

            <div>
              <div className="mb-2 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Thermometer className="h-4 w-4 text-green-600" />
                  <span className="text-sm font-medium text-foreground">토양 온도</span>
                </div>
                <span className="text-sm font-semibold text-foreground">{analysis.statusMetrics.soilTemp}%</span>
              </div>
              <div className="h-2 overflow-hidden rounded-full bg-secondary">
                <div
                  className="h-full rounded-full bg-gradient-to-r from-green-500 to-green-600 transition-all"
                  style={{ width: `${analysis.statusMetrics.soilTemp}%` }}
                />
              </div>
            </div>
            
            <div>
              <div className="mb-2 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Droplets className="h-4 w-4 text-orange-600" />
                  <span className="text-sm font-medium text-foreground">토양 습도</span>
                </div>
                <span className="text-sm font-semibold text-foreground">{analysis.statusMetrics.soilHumi}%</span>
              </div>
              <div className="h-2 overflow-hidden rounded-full bg-secondary">
                <div
                  className="h-full rounded-full bg-gradient-to-r from-orange-500 to-orange-600 transition-all"
                  style={{ width: `${analysis.statusMetrics.soilHumi}%` }}
                />
              </div>
            </div>
          </div>
        </div>
      </Card>

      {/* Issues */}
      {analysis.issues.length > 0 && (
        <Card className="mb-6 border-l-4 border-l-accent bg-accent/5 p-4">
          <div className="flex items-start gap-3">
            <AlertCircle className="h-5 w-5 text-accent" />
            <div>
              <h3 className="mb-1 font-semibold text-foreground">발견된 문제</h3>
              <ul className="space-y-1">
                {analysis.issues.map((issue, index) => (
                  <li key={index} className="text-sm text-muted-foreground">
                    • {issue.message}
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </Card>
      )}

      <div className="mb-6">
        <h2 className="mb-4 text-2xl font-bold text-foreground">종합 분석</h2>
        <div className="space-y-4">
          {/* 현재 환경 정보 */}
          <Card className="border-2 bg-gradient-to-br from-secondary/20 to-accent/10 p-6">
            <div className="mb-4 flex flex-wrap gap-3">
              <Badge variant="secondary" className="gap-1 px-3 py-1 text-sm">
                <Leaf className="h-3 w-3" />
                {analysis.environment.season}
              </Badge>
              <Badge variant="secondary" className="gap-1 px-3 py-1 text-sm">
                <Thermometer className="h-3 w-3" />
                실내 {analysis.environment.indoorTemp}°C
              </Badge>
              <Badge variant="secondary" className="gap-1 px-3 py-1 text-sm">
                <Cloud className="h-3 w-3" />
                습도 {analysis.environment.humidity}%
              </Badge>
              <Badge variant="secondary" className="gap-1 px-3 py-1 text-sm">
                <Sun className="h-3 w-3" />
                {analysis.environment.lightCondition}
              </Badge>
            </div>
            <p className="text-pretty leading-relaxed text-foreground">{analysis.summary}</p>
          </Card>

          <Card className="border-2 border-primary/20 bg-primary/5 p-6">
            <div className="mb-3 flex items-center gap-2">
              <div className="rounded-full bg-primary/20 p-2">
                <TrendingUp className="h-5 w-5 text-primary" />
              </div>
              <h3 className="text-lg font-semibold text-foreground">환경 맞춤 조언</h3>
            </div>
            <p className="text-pretty leading-relaxed text-foreground">{analysis.contextualAdvice}</p>
          </Card>
        </div>
      </div>

      {/* Recommendations */}
      <div className="mb-6">
        <h2 className="mb-4 text-2xl font-bold text-foreground">세부 관리 가이드</h2>
        <div className="grid gap-4 md:grid-cols-3">
          {analysis.recommendations.map((rec, index) => {
            const Icon = rec.icon
            return (
              <Card key={index} className="border-2 p-5 transition-all hover:border-primary hover:shadow-md">
                <div className="mb-3 flex items-center justify-between">
                  <div className="rounded-full bg-primary/10 p-2">
                    <Icon className="h-5 w-5 text-primary" />
                  </div>
                  <Badge
                    variant={rec.priority === "high" ? "default" : rec.priority === "medium" ? "secondary" : "outline"}
                    className="text-xs"
                  >
                    {rec.priority === "high" ? "중요" : rec.priority === "medium" ? "보통" : "참고"}
                  </Badge>
                </div>
                <h3 className="mb-2 font-semibold text-foreground">{rec.title}</h3>
                <p className="text-sm leading-relaxed text-muted-foreground">{rec.description}</p>
              </Card>
            )
          })}
        </div>
      </div>

      {/* Similar Images */}
      <div>
        <h2 className="mb-4 text-2xl font-bold text-foreground">유사한 식물 사례</h2>
        <Card className="border-2 p-6">
          <p className="mb-4 text-sm text-muted-foreground">현재 환경과 유사한 조건에서 관리되고 있는 식물들입니다</p>
          <div className="grid grid-cols-3 gap-4">
            {analysis.similarImages.map((image, index) => (
              <div
                key={index}
                className="overflow-hidden rounded-lg border-2 border-border transition-all hover:border-primary hover:shadow-md"
              >
                <img
                  src={image || "/placeholder.svg"}
                  alt={`Similar plant ${index + 1}`}
                  className="aspect-square w-full object-cover"
                />
              </div>
            ))}
          </div>
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
