// Healthcheck của container frontend (Compose gọi trực tiếp, không qua Nginx).
export function GET() {
  return Response.json({ status: "UP" });
}
