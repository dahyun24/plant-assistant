/** @type {import('next').NextConfig} */
const nextConfig = {
  typescript: {
    ignoreBuildErrors: true,
  },
  images: {
    unoptimized: true,
  },

  async rewrites() {
    return [
      {
        source: '/api/proxy/:path*', // 프론트엔드에서 호출할 주소 (예: /api/proxy/v1/plants/analyze)
        destination: 'http://localhost:8080/:path*', // 실제 백엔드 주소
      },
    ];
  },
}

export default nextConfig
