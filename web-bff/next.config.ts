import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /* config options here */
  cacheComponents: true,
  partialPrefetching: true,
  // Solo desarrollo: hCaptcha no acepta localhost, se prueba con un nombre en el archivo hosts (AQ-117)
  allowedDevOrigins: ["test.mydomain.com"],
};

export default nextConfig;
