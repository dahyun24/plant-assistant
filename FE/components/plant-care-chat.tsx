"use client"

import type React from "react"
import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Textarea } from "@/components/ui/textarea"
import { Label } from "@/components/ui/label"
import { Leaf, Sparkles, ImageIcon, Plus, History, MessageSquare } from "lucide-react"
import { toast } from "sonner"
import PlantAnalysisResult from "@/components/plant-analysis-result"
import { ApiResponse, PlantAnalysisResultData, HistoryItem, HistoryListResponse } from "@/types/plant"
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarProvider,
  SidebarTrigger,
  SidebarFooter,
} from "@/components/ui/sidebar"
import { Separator } from "@/components/ui/separator"

export default function PlantCareChat() {
  const [message, setMessage] = useState("")
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [previewImage, setPreviewImage] = useState<string | null>(null)
  const [showAnalysis, setShowAnalysis] = useState(false)
  const [isAnalyzing, setIsAnalyzing] = useState(false)
  
  // 분석 결과 데이터 상태
  const [resultData, setResultData] = useState<PlantAnalysisResultData | null>(null)

  // [추가] 히스토리 목록 상태
  const [historyList, setHistoryList] = useState<HistoryItem[]>([])
  const [isLoadingHistory, setIsLoadingHistory] = useState(false)

  // [추가] 컴포넌트 마운트 시 히스토리 불러오기
  useEffect(() => {
    fetchHistory()
  }, [])

  // [추가] 히스토리 목록 API 호출
  const fetchHistory = async () => {
    try {
      const res = await fetch("/api/proxy/v1/plants/history")
      const data: HistoryListResponse = await res.json()
      if (data.isSuccess) {
        setHistoryList(data.result)
      }
    } catch (error) {
      console.error("히스토리 로딩 실패:", error)
    }
  }

  // [추가] 히스토리 클릭 시 상세 내용 불러오기
  const handleHistoryClick = async (id: number) => {
    setIsLoadingHistory(true)
    try {
      const res = await fetch(`/api/proxy/v1/plants/history/${id}`)
      const data: ApiResponse = await res.json()

      if (data.isSuccess) {
        setResultData(data.result)
        setShowAnalysis(true)
      } else {
        alert("기록을 불러오지 못했습니다.")
      }
    } catch (error) {
      console.error("상세 조회 오류:", error)
      alert("상세 내용을 불러오는 중 오류가 발생했습니다.")
    } finally {
      setIsLoadingHistory(false)
    }
  }

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      setSelectedFile(file)
      const reader = new FileReader()
      reader.onloadend = () => {
        setPreviewImage(reader.result as string)
      }
      reader.readAsDataURL(file)
    }
  }

  const handleAnalyze = async () => {
    if (!selectedFile) return

    setIsAnalyzing(true)

    try {
      const formData = new FormData()
      formData.append("image", selectedFile)
      if (message) {
        formData.append("description", message)
      }

      // Next.js Rewrite를 통해 백엔드로 요청
      const response = await fetch("/api/proxy/v1/plants/analyze", {
        method: "POST",
        body: formData,
      })

      if (!response.ok) {
        throw new Error(`서버 오류: ${response.status}`)
      }

      const data: ApiResponse = await response.json()

      if (data.isSuccess) {
        setResultData(data.result)
        setShowAnalysis(true)
        fetchHistory() // [추가] 분석 완료 후 히스토리 목록 갱신
      } else {
        alert("분석 실패: " + data.message)
      }
    } catch (error) {
      console.error("분석 중 오류 발생:", error)
      alert("분석 중 오류가 발생했습니다. 다시 시도해주세요.")
    } finally {
      setIsAnalyzing(false)
    }
  }

  // 초기화 (새로운 분석 시작)
  const handleReset = () => {
    setShowAnalysis(false)
    setPreviewImage(null)
    setSelectedFile(null)
    setMessage("")
    setResultData(null)
  }

  return (
    // [변경] 전체를 SidebarProvider로 감쌈
    <SidebarProvider>
      <div className="flex min-h-screen w-full bg-background">
        
        {/* [추가] 사이드바 컴포넌트 */}
        <Sidebar>
          <SidebarHeader className="p-4">
            <div className="flex items-center gap-2 px-2 mb-2">
              <Leaf className="h-6 w-6 text-primary" />
              <span className="text-lg font-bold">Plant AI</span>
            </div>
            <Button onClick={handleReset} className="w-full justify-start gap-2" variant="outline">
              <Plus className="h-4 w-4" />
              새로운 분석
            </Button>
          </SidebarHeader>
          
          <Separator />
          
          <SidebarContent>
            <SidebarGroup>
              <SidebarGroupLabel>최근 분석 기록</SidebarGroupLabel>
              <SidebarGroupContent>
                <SidebarMenu>
                  {historyList.length === 0 ? (
                    <div className="px-4 py-8 text-center text-sm text-muted-foreground">
                      <History className="mx-auto h-8 w-8 mb-2 opacity-50" />
                      <p>아직 기록이 없습니다.</p>
                    </div>
                  ) : (
                    historyList.map((item) => (
                      <SidebarMenuItem key={item.id}>
                        <SidebarMenuButton 
                          onClick={() => handleHistoryClick(item.id)}
                          className="h-auto py-3 px-3"
                        >
                          <div className="flex flex-col gap-1 text-left w-full overflow-hidden">
                            <div className="flex items-center justify-between w-full">
                              <span className="font-medium truncate text-sm">
                                {item.plantName}
                              </span>
                              <span className="text-[10px] text-muted-foreground shrink-0">
                                {new Date(item.createdAt).toLocaleDateString()}
                              </span>
                            </div>
                            <div className="flex items-center gap-2">
                                <span className={`text-[10px] px-1.5 py-0.5 rounded-full ${
                                    item.growthLevel === 'High' ? 'bg-green-100 text-green-700' :
                                    item.growthLevel === 'Medium' ? 'bg-yellow-100 text-yellow-700' :
                                    'bg-red-100 text-red-700'
                                }`}>
                                    {item.growthLevel}
                                </span>
                                <span className="text-xs text-muted-foreground truncate block flex-1">
                                  {item.userDescription || "이미지 분석"}
                                </span>
                            </div>
                          </div>
                        </SidebarMenuButton>
                      </SidebarMenuItem>
                    ))
                  )}
                </SidebarMenu>
              </SidebarGroupContent>
            </SidebarGroup>
          </SidebarContent>
          <SidebarFooter className="p-4 text-xs text-center text-muted-foreground">
              Plant Care Assistant © 2025
          </SidebarFooter>
        </Sidebar>

        {/* [변경] 메인 콘텐츠 영역 */}
        <main className="flex-1 flex flex-col min-w-0 overflow-hidden relative">
          {/* 모바일용 사이드바 트리거 */}
          <div className="p-4 md:hidden absolute top-0 left-0 z-10">
            <SidebarTrigger />
          </div>

          {/* 로딩 오버레이 */}
          {isLoadingHistory && (
             <div className="absolute inset-0 z-50 flex items-center justify-center bg-background/80 backdrop-blur-sm">
               <div className="flex flex-col items-center gap-2">
                 <Sparkles className="h-8 w-8 animate-spin text-primary" />
                 <p>기록 불러오는 중...</p>
               </div>
             </div>
          )}

          {showAnalysis && resultData ? (
            <div className="h-full overflow-y-auto">
                <PlantAnalysisResult data={resultData} onReset={handleReset} />
            </div>
          ) : (
            <div className="container mx-auto max-w-4xl px-4 py-8 md:py-12 flex-1 overflow-y-auto">
              {/* Header */}
              <div className="mb-8 text-center pt-8 md:pt-0">
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
                        {previewImage ? (
                          <div className="relative h-full w-full flex justify-center bg-black/5 rounded-lg">
                            <img
                              src={previewImage || "/placeholder.svg"}
                              alt="Uploaded plant"
                              className="max-h-[400px] w-auto rounded-lg object-contain"
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
                    disabled={!selectedFile || isAnalyzing}
                    className="h-12 w-full text-base font-semibold shadow-md transition-all hover:shadow-lg disabled:opacity-50"
                    size="lg"
                  >
                    {isAnalyzing ? (
                      <>
                        <Sparkles className="mr-2 h-5 w-5 animate-spin" />
                        분석 중... (최대 1분 소요)
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
              <div className="mt-8 grid gap-4 md:grid-cols-3 mb-8">
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
          )}
        </main>
      </div>
    </SidebarProvider>
  )
}