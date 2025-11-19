"use client"

import type React from "react"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Textarea } from "@/components/ui/textarea"
import { Label } from "@/components/ui/label"
import { Leaf, Sparkles, ImageIcon } from "lucide-react"
import PlantAnalysisResult from "@/components/plant-analysis-result"

export default function PlantCareChat() {
  const [message, setMessage] = useState("")
  const [uploadedImage, setUploadedImage] = useState<string | null>(null)
  const [showAnalysis, setShowAnalysis] = useState(false)
  const [isAnalyzing, setIsAnalyzing] = useState(false)
  const [detectedPlant, setDetectedPlant] = useState<string>("")

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      const reader = new FileReader()
      reader.onloadend = () => {
        setUploadedImage(reader.result as string)
      }
      reader.readAsDataURL(file)
    }
  }

  const handleAnalyze = () => {
    if (!uploadedImage) return

    setIsAnalyzing(true)
    setTimeout(() => {
      setDetectedPlant("보스턴고사리")
      setIsAnalyzing(false)
      setShowAnalysis(true)
    }, 2000)
  }

  const handleReset = () => {
    setShowAnalysis(false)
    setUploadedImage(null)
    setMessage("")
    setDetectedPlant("")
  }

  if (showAnalysis) {
    return <PlantAnalysisResult plantType={detectedPlant} onReset={handleReset} />
  }

  return (
    <div className="container mx-auto max-w-4xl px-4 py-8 md:py-12">
      {/* Header */}
      <div className="mb-8 text-center">
        <div className="mb-4 flex items-center justify-center gap-2">
          <div className="rounded-full bg-primary p-3">
            <Leaf className="h-8 w-8 text-primary-foreground" />
          </div>
        </div>
        <h1 className="mb-2 text-balance text-4xl font-bold tracking-tight text-foreground md:text-5xl">
          식물 케어 도우미
        </h1>
        <p className="text-pretty text-lg text-muted-foreground">식물 사진을 업로드하고 상태를 분석받아보세요</p>
      </div>

      {/* Main Card */}
      <Card className="overflow-hidden border-2 shadow-lg">
        <div className="bg-gradient-to-br from-primary/5 to-accent/5 p-6 md:p-8">
          {/* Image Upload */}
          <div className="mb-6">
            <Label htmlFor="plant-image" className="mb-2 text-base font-semibold text-foreground">
              식물 사진 업로드
            </Label>
            <div className="relative">
              <input type="file" id="plant-image" accept="image/*" onChange={handleImageUpload} className="hidden" />
              <label
                htmlFor="plant-image"
                className="flex min-h-[200px] cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed border-border bg-card transition-colors hover:border-primary hover:bg-secondary/50"
              >
                {uploadedImage ? (
                  <div className="relative h-full w-full">
                    <img
                      src={uploadedImage || "/placeholder.svg"}
                      alt="Uploaded plant"
                      className="h-full w-full rounded-lg object-cover"
                    />
                    <div className="absolute inset-0 flex items-center justify-center rounded-lg bg-black/40 opacity-0 transition-opacity hover:opacity-100">
                      <p className="text-sm font-medium text-white">클릭하여 변경</p>
                    </div>
                  </div>
                ) : (
                  <div className="flex flex-col items-center gap-3 p-8 text-center">
                    <div className="rounded-full bg-primary/10 p-4">
                      <ImageIcon className="h-8 w-8 text-primary" />
                    </div>
                    <div>
                      <p className="text-base font-medium text-foreground">사진을 업로드하세요</p>
                      <p className="mt-1 text-sm text-muted-foreground">클릭하거나 드래그하여 이미지 추가</p>
                    </div>
                  </div>
                )}
              </label>
            </div>
          </div>

          {/* Text Input */}
          <div className="mb-6">
            <Label htmlFor="message" className="mb-2 text-base font-semibold text-foreground">
              추가 정보 (선택사항)
            </Label>
            <Textarea
              id="message"
              placeholder="식물의 상태나 궁금한 점을 자유롭게 작성해주세요..."
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              className="min-h-[120px] resize-none bg-card text-base"
            />
          </div>

          {/* Action Button */}
          <Button
            onClick={handleAnalyze}
            disabled={!uploadedImage || isAnalyzing}
            className="h-12 w-full text-base font-semibold shadow-md transition-all hover:shadow-lg disabled:opacity-50"
            size="lg"
          >
            {isAnalyzing ? (
              <>
                <Sparkles className="mr-2 h-5 w-5 animate-spin" />
                분석 중...
              </>
            ) : (
              <>
                <Sparkles className="mr-2 h-5 w-5" />
                식물 상태 분석하기
              </>
            )}
          </Button>
        </div>

        {/* Info Section */}
        <div className="border-t bg-muted/30 p-6">
          <div className="flex items-start gap-3">
            <div className="rounded-full bg-accent/20 p-2">
              <Sparkles className="h-5 w-5 text-accent" />
            </div>
            <div>
              <h3 className="mb-1 font-semibold text-foreground">AI 기반 자동 분석</h3>
              <p className="text-sm leading-relaxed text-muted-foreground">
                업로드한 사진을 AI가 자동으로 분석하여 식물 종류를 식별하고, 현재 상태를 진단하여 유사한 사례와 맞춤형
                관리 가이드를 제공합니다.
              </p>
            </div>
          </div>
        </div>
      </Card>

      {/* Features */}
      <div className="mt-8 grid gap-4 md:grid-cols-3">
        <Card className="border bg-card p-4">
          <div className="mb-2 flex items-center gap-2">
            <div className="rounded-full bg-primary/10 p-2">
              <ImageIcon className="h-4 w-4 text-primary" />
            </div>
            <h3 className="font-semibold text-card-foreground">간편한 업로드</h3>
          </div>
          <p className="text-sm leading-relaxed text-muted-foreground">사진과 텍스트로 쉽게 식물 상태를 공유하세요</p>
        </Card>

        <Card className="border bg-card p-4">
          <div className="mb-2 flex items-center gap-2">
            <div className="rounded-full bg-primary/10 p-2">
              <Sparkles className="h-4 w-4 text-primary" />
            </div>
            <h3 className="font-semibold text-card-foreground">자동 식별</h3>
          </div>
          <p className="text-sm leading-relaxed text-muted-foreground">AI가 식물 종류를 자동으로 인식하고 분석합니다</p>
        </Card>

        <Card className="border bg-card p-4">
          <div className="mb-2 flex items-center gap-2">
            <div className="rounded-full bg-primary/10 p-2">
              <Leaf className="h-4 w-4 text-primary" />
            </div>
            <h3 className="font-semibold text-card-foreground">맞춤 가이드</h3>
          </div>
          <p className="text-sm leading-relaxed text-muted-foreground">식물별 최적의 관리 방법을 제안합니다</p>
        </Card>
      </div>
    </div>
  )
}
