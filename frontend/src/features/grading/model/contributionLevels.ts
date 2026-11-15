export const CONTRIBUTION_LEVELS = [
  {
    value: "EXCEEDS_EXPECTATIONS",
    label: "Vượt kỳ vọng",
    description: "Hoàn thành tốt phần được giao, hiểu rõ bài, có đóng góp thêm hoặc hỗ trợ nhóm.",
  },
  {
    value: "MEETS_EXPECTATIONS",
    label: "Đạt yêu cầu",
    description: "Hoàn thành đầy đủ phần được giao, chất lượng đạt yêu cầu và giải thích được cách làm.",
  },
  {
    value: "PARTIALLY_MEETS",
    label: "Đạt một phần",
    description: "Có đóng góp nhưng còn thiếu nội dung, chất lượng chưa đạt hoặc giải thích chưa rõ.",
  },
  {
    value: "NEEDS_SUPPORT",
    label: "Cần hỗ trợ nhiều",
    description: "Cần được hướng dẫn, nhắc nhở nhiều mới hoàn thành; chưa tự giải thích được phần mình làm.",
  },
  {
    value: "NOT_COMPLETED",
    label: "Không hoàn thành",
    description: "Không hoàn thành phần được giao hoặc không có đóng góp được xác nhận.",
  },
] as const;

export type ContributionLevel = (typeof CONTRIBUTION_LEVELS)[number]["value"];
