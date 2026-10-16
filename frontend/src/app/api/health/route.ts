// Healthcheck của container frontend; Nginx chuyển /api/* sang backend nên route này chỉ gọi được trong container.
export function GET() {
  return Response.json({ status: "UP" });
}
